package com.jamhour.core.provider.impl.scraped

import com.jamhour.core.job.Job
import com.jamhour.core.job.buildJob
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.sendAsync
import com.jamhour.util.toURI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class Foras(expiryDuration: Duration = 3.hours) : AbstractJobsProvider(
    "Foras.ps",
    "Ramallah, Palestine",
    "https://foras.ps".toURI(),
    expiryDuration
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getJobs(): Flow<Job> = flow {
        logger.info { "Fetching job listings from $providerName at $FORAS_JOB_URL" }

        val html = fetchJobListingsHtml(FORAS_JOB_URL)
        val elements = parseJobElements(html)
        logger.info { "Found ${elements.size} jobs to process" }

        val concurrentConvert = elements.asFlow()
            .flatMapMerge { element ->
                flowOf(
                    runCatching { processJobElement(element) }
                        .onFailure { e -> logger.severe { "Error processing job element: ${e.message}" } }
                        .getOrNull()
                )
            }.filterNotNull()
        emitAll(concurrentConvert)
    }.catch { e ->
        providerStatusProperty = JobProviderStatus.FAILED
        logger.severe { "Flow failed: ${e.message}" }
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
        val deadlineStr = extractJobDeadline(jobElement, jobLocation)

        val deadline = runCatching {
            LocalDate.parse(deadlineStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }.onFailure {
            logger.severe { "Error parsing deadline '$deadlineStr': ${it.message}" }
        }.getOrNull() ?: return null.also {
            logger.info { "Skipping job with invalid deadline: '$deadlineStr'" }
        }

        if (deadline.isBefore(LocalDate.now())) {
            logger.info { "Skipping expired job (deadline: $deadline)" }
            return null
        }

        val jobTitle = extractJobTitle(jobElement)
        val jobDetailsLink = extractJobDetailsLink(jobElement)
        val fullJobLink = providerURI?.resolve(jobDetailsLink) ?: return null
        val description = extractJobDescription(jobElement)

        logger.info { "Processed job: '$jobTitle' at '$jobLocation', Deadline: $deadline" }

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
        jobElement.getElementsByTag("a")
            .firstOrNull { it.hasAttr("href") && it.attr("href").startsWith("/foras/") }
            ?.attr("href").orEmpty()

    private fun extractJobDescription(jobElement: Element) =
        jobElement.getElementsByClass("line-clamp-2 md:line-clamp-3 text-sm h-[2.5rem] md:h-[3.5rem] mb-2")
            .firstOrNull()?.text().orEmpty()

    companion object {
        private const val FORAS_JOB_URL =
            "https://foras.ps/opportunities?category=3&major=1,11&datePosted=AnyTime&orderBy=date"
        private const val JOB_CONTAINER_SELECTOR =
            "body > div.bg-primary-gray > main > div > div.container.mx-auto.mt-0 > div.row.no-gutters.gap-x-8.gap-y-5 > div.col-md-8 > div.grid.md\\:grid-cols-2.grid-cols-1.gap-x-5.gap-y-5"
    }
}
