package com.jamhour.core.provider.impl.scraped

import com.jamhour.core.job.Job
import com.jamhour.core.job.buildJob
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.util.sendAsync
import com.jamhour.util.toURI
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Foras : AbstractJobsProvider(
    "Foras.ps",
    "Ramallah, Palestine",
    "https://foras.ps".toURI()
) {

    override suspend fun getJobs(): List<Job> = coroutineScope {
        logger.info { "Fetching job listings from $providerName at $FORAS_JOB_URL" }

        val jobListingsHtml = fetchJobListingsHtml(FORAS_JOB_URL)
        val jobElements = parseJobElements(jobListingsHtml)
        logger.info { "Found ${jobElements.size} job elements to process" }

        jobElements.mapNotNull { jobElement ->
            runCatching { processJobElement(jobElement) }
                .onFailure { logger.severe { "Error processing job element: ${jobElement.text()}" } }
                .getOrNull()
        }
    }

    private suspend fun fetchJobListingsHtml(url: String): String {
        logger.info { "Sending request to fetch job listings from $url" }
        return HttpRequest.newBuilder()
            .uri(url.toURI())
            .GET()
            .build()
            .sendAsync(HttpResponse.BodyHandlers.ofString())
    }

    private fun parseJobElements(html: String): List<Element> {
        logger.info { "Parsing job elements from HTML" }
        val document = Jsoup.parse(html, FORAS_JOB_URL)
        return document.select(JOB_CONTAINER_SELECTOR).flatMap {
            it.getElementsByClass("col-9 flex flex-col gap-y-2")
        }
    }

    private fun processJobElement(jobElement: Element): Job? {
        val jobLocation = extractJobLocation(jobElement)
        val deadline = extractJobDeadline(jobElement, jobLocation).let {
            runCatching {
                LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }.onFailure {
                logger.severe { "Error parsing deadline: $it" }
            }.getOrNull()
        }

        if (deadline == null) {
            logger.info { "Skipping job due to missing deadline" }
            return null
        }

        if (deadline.isBefore(LocalDate.now())) {
            logger.info { "Skipping job because deadline has passed: $deadline" }
            return null
        }

        val jobTitle = extractJobTitle(jobElement)
        val jobDetailsLink = extractJobDetailsLink(jobElement)
        val fullJobLink = providerURI?.let { "$it$jobDetailsLink".toURI() } ?: return null
        val description = extractJobDescription(jobElement)

        logger.info { "Processing job: $jobTitle at $jobLocation, Deadline: $deadline" }

        return buildJob(getDefaultJobPoster(), fullJobLink, jobTitle, jobLocation) {
            jobDeadline = deadline
            jobDescription = description
        }
    }

    private fun extractJobTitle(jobElement: Element) =
        jobElement.getElementsByClass("text-base font-bold line-clamp-2")
            .firstOrNull()?.text().orEmpty()

    private fun extractJobLocation(jobElement: Element) =
        jobElement.getElementsByClass("text-xs text-gray-500")
            .firstOrNull()?.text().orEmpty()

    private fun extractJobDeadline(jobElement: Element, jobLocation: String?) =
        jobElement.getElementsByClass("flex flex-col mt-2")
            .firstOrNull()?.text()
            ?.substringBefore(jobLocation ?: "")
            ?.substringAfter("Deadline: ")
            ?.trim().orEmpty()

    private fun extractJobDetailsLink(jobElement: Element) =
        jobElement.getElementsByTag("a").firstOrNull()?.attr("href").orEmpty()

    private fun extractJobDescription(jobElement: Element) =
        jobElement.getElementsByClass("line-clamp-2 md:line-clamp-3 text-sm h-[2.5rem] md:h-[3.5rem] mb-2")
            .firstOrNull()?.text().orEmpty()

    companion object {
        private const val FORAS_JOB_URL =
            "https://foras.ps/Foras?SearchFilter.Filter.Search=&SearchFilter.Specialty=1&SearchFilter.Specialty=8&SearchFilter.Specialty=11&SearchFilter.Category=3&SearchFilter.Filter.DatePosted=AnyTime&SearchFilter.Filter.OrderBy=date&culture=en"
        private const val JOB_CONTAINER_SELECTOR =
            "body > div.bg-primary-gray > main > div > div.container.mx-auto.mt-0 > div.row.no-gutters.gap-x-8.gap-y-5 > div.col-md-8 > div.grid.md\\:grid-cols-2.grid-cols-1.gap-x-5.gap-y-5"
    }
}
