package com.jamhour.providers.impl

import com.jamhour.core.job.Job
import com.jamhour.core.job.JobVacancyType
import com.jamhour.core.job.impl.buildJob
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.impl.buildJobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.sendAsync
import com.jamhour.util.toURI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import java.util.logging.Level
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class JobsDotPS(expiryDuration: Duration = 3.hours) : AbstractJobsProvider(
    "Jobs.ps",
    "Ramallah",
    "https://www.jobs.ps/".toURI(),
    expiryDuration
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getJobs(): Flow<Job> = flow {
        logger.info { "Fetching job listings from $providerName at $IT_JOBS_PAGE_ON_PROVIDER" }

        val html = fetchJobListings()
        val listings = parseJobListings(html)
        logger.info { "Parsed ${listings.size} job listings from $providerName" }

        providerStatusProperty = JobProviderStatus.ACTIVE

        val concurrentConvert = listings.asFlow()
            .flatMapMerge { listing ->
                flowOf(
                    runCatching { processJobListing(listing) }
                        .onFailure { e ->
                            logger.severe { "Error processing job at URL ${listing.attr("href")}: ${e.message}" }
                        }
                        .getOrNull()
                ).filterNotNull()
            }

        emitAll(concurrentConvert)
    }.catch { e ->
        providerStatusProperty = JobProviderStatus.FAILED
        logger.severe { "Error in flow: ${e.message}" }
    }


    private suspend fun fetchJobListings(): String {
        logger.info { "Fetching job listings HTML from $IT_JOBS_PAGE_ON_PROVIDER" }
        return HttpRequest.newBuilder()
            .uri(IT_JOBS_PAGE_ON_PROVIDER.toURI())
            .GET()
            .build()
            .sendAsync(HttpResponse.BodyHandlers.ofString())
            .also {
                logger.info { "Successfully fetched HTML from $IT_JOBS_PAGE_ON_PROVIDER" }
            }
    }

    private fun parseJobListings(html: String): List<Element> {
        logger.info { "Parsing job listings from fetched HTML." }
        val parsedList = Jsoup.parse(html, IT_JOBS_PAGE_ON_PROVIDER).getElementsByClass(JOB_LIST_CLASS_NAME)
        logger.info { "Found ${parsedList.size} job listings." }
        return parsedList
    }

    private suspend fun processJobListing(jobListing: Element): Job? {
        val jobDetailsUrl = jobListing.attr("href")
        val fullJobUrl = providerURI?.resolve(jobDetailsUrl).toString()
        logger.info { "Processing job listing at URL: $jobDetailsUrl" }

        val jobDetailsHtml = fetch(jobDetailsUrl)
        val jobDetailsDoc = Jsoup.parse(jobDetailsHtml, jobDetailsUrl)

        if (isJobExpired(jobDetailsDoc)) {
            logger.info { "Skipping expired job at URL: $jobDetailsUrl" }
            return null
        }

        val publishDate = parsePublishDate(jobListing) ?: run {
            logger.warning { "Publish date missing or invalid at URL: $fullJobUrl. Skipping job." }
            return null
        }

        val jobPoster = fetchJobPoster(jobDetailsDoc) ?: run {
            logger.warning { "Job poster details missing at URL: $fullJobUrl. Skipping job." }
            return null
        }

        logger.info { "Building job object for listing at URL: $jobDetailsUrl" }
        return buildJobFromDetails(jobDetailsDoc, jobPoster, jobDetailsUrl, publishDate)
    }

    private suspend fun fetch(url: String): String {
        logger.info { "Fetching content from URL: $url" }
        return HttpRequest.newBuilder(url.toURI())
            .build()
            .sendAsync(HttpResponse.BodyHandlers.ofString())
            .also {
                logger.info { "Successfully fetched content from URL: $url" }
            }
    }

    private fun isJobExpired(doc: Document) = doc.selectFirst(EXPIRED_SELECTOR) != null

    private fun parsePublishDate(element: Element): LocalDate? {
        val dateText = element.selectFirst(JOB_PUBLISH_DATE_HOME_PAGE)?.text()
        return dateText?.let {
            runCatching {
                parseLocalDateFromProvider(dateText)
            }.onFailure {
                logger.log(Level.WARNING, it) { "Failed to parse publish date: $it. Error: ${it.message}" }
            }.getOrNull()
        }
    }

    private fun getStringMonthMap(): Map<String, Month> {
        return Month.entries.asSequence()
            .associateBy { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    private fun parseLocalDateFromProvider(jobPublishDate: String): LocalDate {
        val months = getStringMonthMap()

        val split = jobPublishDate.split(", ")
        val monthIndex = 1
        val dayIndex = 0

        return LocalDate.of(
            Year.now().value,
            months[split[monthIndex]],
            split[dayIndex].toInt()
        )
    }

    private suspend fun fetchJobPoster(jobDetailsDoc: Document): JobPoster? = coroutineScope {
        val posterPageLink =
            jobDetailsDoc.selectFirst(JOB_POSTER_PAGE_LINK_SELECTOR)?.attr("href")
                ?: return@coroutineScope null

        logger.info { "Fetching job poster details from $posterPageLink" }
        val posterPageHtml = fetch(posterPageLink)
        val posterPageDoc = Jsoup.parse(posterPageHtml, posterPageLink)

        buildJobPosterFromPage(posterPageDoc, posterPageLink)
    }

    private fun buildJobPosterFromPage(doc: Document, posterPageLink: String): JobPoster? {
        // ! TODO: there's a bug here where a job poster with a description fails to parse correctly due to selectors
        val companyTitle = doc.selectFirst(COMPANY_TITLE_SELECTOR)?.text() ?: return null
        val companyWebsite = doc.selectFirst(COMPANY_WEBSITE_SELECTOR)?.text() ?: return null
        val companyLocation = doc.selectFirst(COMPANY_LOCATION_SELECTOR)?.text() ?: return null
        val companyEstablishmentDate =
            doc.selectFirst(COMPANY_ESTABLISHMENT_DATE_SELECTOR)?.text()?.trim() ?: return null

        logger.info { "Building JobPoster for company: $companyTitle at $companyWebsite" }

        return buildJobPoster(this@JobsDotPS, companyTitle, companyLocation) {
            website = companyWebsite.toURI()
            uriOnProvider = posterPageLink.toURI()
            establishmentDate = when {
                companyEstablishmentDate.isBlank() -> null
                else -> LocalDate.parse(companyEstablishmentDate)
            }
        }
    }

    private fun buildJobFromDetails(
        doc: Document, jobPoster: JobPoster, jobUrl: String, publishDate: LocalDate
    ): Job? {
        val descriptionText = doc.selectFirst(DESCRIPTION_TAG_SELECTOR)?.text()
            ?.replace(" Jobs.ps, Ltd. All Rights Reserved.", "")
            ?: return null
        val requirementsText = doc.selectFirst(REQUIREMENTS_TAG_SELECTOR)?.text() ?: return null
        val jobDeadlineText = doc.selectFirst(JOB_DEADLINE_SELECTOR)?.text() ?: return null
        val jobVacancyTypeText = doc.selectFirst(JOB_VACANCY_TYPE_SELECTOR)?.text() ?: return null
        val jobTitleText = doc.selectFirst(JOB_TITLE_SELECTOR)?.text() ?: return null
        val jobLocationText = doc.selectFirst(JOB_LOCATION_SELECTOR)?.text() ?: return null
        val positionLevelText = doc.selectFirst(POSITION_LEVEL_SELECTOR)?.text() ?: return null
        val degreeText = doc.selectFirst(DEGREE_SELECTOR)?.text() ?: return null
        val salaryText = doc.selectFirst(SALARY_SELECTOR)?.text() ?: return null
        val experienceText = doc.selectFirst(EXPERIENCE_SELECTOR)?.text() ?: return null
        val jobInstructionsText = doc.select(JOB_INSTRUCTIONS_ON_HOW_TO_APPLY_SELECTOR).text()

        logger.info { "Building job object from details at URL: $jobUrl" }
        return buildJob(
            jobPoster,
            jobUrl.toURI(),
            jobTitleText,
            jobLocationText
        ) {
            jobDescription = descriptionText
            jobRequirements = requirementsText
            jobDeadline = LocalDate.parse(jobDeadlineText, deadLineFormatter)
            jobInstructionsOnHowToApply = jobInstructionsText
            typeOfVacancy = getTypeOfVacancy(jobVacancyTypeText)
            jobSalary = salaryText
            jobDegree = degreeText
            jobPositionLevel = positionLevelText
            jobExperience = experienceText
            jobPublishDate = publishDate
        }
    }

    private fun getTypeOfVacancy(jobTypeKey: String?): JobVacancyType {
        return when (jobTypeKey) {
            "Full time" -> JobVacancyType.FULL_TIME
            "Part time" -> JobVacancyType.PART_TIME
            "Contract and Consultation" -> JobVacancyType.CONTRACTOR
            "Part time and Full time" -> JobVacancyType.FULL_TIME
            else -> JobVacancyType.NOT_SPECIFIED
        }
    }

    companion object {
        private val deadLineFormatter = DateTimeFormatter.ofPattern("dd - MMM - yyyy")
        private const val IT_JOBS_PAGE_ON_PROVIDER = "https://www.jobs.ps/en/categories/it-jobs"
        private const val JOB_LIST_CLASS_NAME = "list-3--title list-3--row"
        private const val JOB_PUBLISH_DATE_HOME_PAGE = ".list-3--cell-1.list-3--cell-4.align-right"
        private const val EXPIRED_SELECTOR = ".btn-3.btn-block.view--job-post--apply.expired"
        private const val JOB_POSTER_PAGE_LINK_SELECTOR = ".view--title h2 a"
        private const val DESCRIPTION_TAG_SELECTOR = ".view--content.readable-content > div:nth-child(2)"
        private const val REQUIREMENTS_TAG_SELECTOR = ".view--content.readable-content > div:nth-child(4)"
        private const val JOB_TITLE_SELECTOR = ".view--detail-custom > div:nth-child(1) > span:nth-child(2)"
        private const val JOB_LOCATION_SELECTOR = ".view--detail-item.view--detail-item-location > span:nth-child(2)"
        private const val JOB_DEADLINE_SELECTOR = ".view--detail-custom > div:nth-child(2) > span:nth-child(2)"
        private const val JOB_VACANCY_TYPE_SELECTOR = ".view--detail-custom > div:nth-child(4) > span:nth-child(2)"
        private const val JOB_INSTRUCTIONS_ON_HOW_TO_APPLY_SELECTOR =
            ".view--content.readable-content > div:nth-child(9)"
        private const val POSITION_LEVEL_SELECTOR = ".view--detail-custom > div:nth-child(5) > span:nth-child(2)"
        private const val DEGREE_SELECTOR = ".view--detail-custom > div:nth-child(7) > span:nth-child(2)"
        private const val SALARY_SELECTOR = ".view--detail-item.view--detail-item--salary > span:nth-child(2)"
        private const val EXPERIENCE_SELECTOR = ".view--detail-custom > div:nth-child(8) > span:nth-child(2)"
        private const val COMPANY_TITLE_SELECTOR = ".f-wrapper .view--header h1 > span"
        private const val COMPANY_WEBSITE_SELECTOR = "#tab-1 > div:nth-child(1) > span.value"
        private const val COMPANY_LOCATION_SELECTOR = "#tab-1 .company-profile--locations > span.value"
        private const val COMPANY_ESTABLISHMENT_DATE_SELECTOR = "#tab-1 .company-profile--foundation_date > span.value"
    }
}