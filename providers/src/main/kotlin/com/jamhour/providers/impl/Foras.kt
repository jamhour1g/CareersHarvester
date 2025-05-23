package com.jamhour.providers.impl

import com.jamhour.core.job.Job
import com.jamhour.core.job.impl.buildJob
import com.jamhour.core.poster.impl.buildJobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.sendAsync
import com.jamhour.util.toBodyHandler
import com.jamhour.util.toURI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jsoup.Jsoup
import java.net.http.HttpRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class Foras(expiryDuration: Duration = 3.hours) : AbstractJobsProvider(
    "Foras.ps",
    "Ramallah, Palestine",
    "https://foras.ps".toURI(),
    expiryDuration
) {

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }

    private val requestBody = """
        {
            "pageNumber": 1,
            "pageSize": 10,
            "category": "3",
            "major": [1, 11],
            "datePosted": "AnyTime",
            "orderBy": "date",
            "categories": [3]
        }
    """.trimIndent()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getJobs(): Flow<Job> = flow {
        logger.info { "Fetching job listings from $providerName at $FORAS_JOB_URL" }

        val jobsResponse = fetchJobs()
        val jobs = jobsResponse?.result?.mapNotNull { it.mapToJob() } ?: emptyList()

        if (jobs.isEmpty()) {
            logger.warning { "No jobs found at $providerName." }
        }

        emitAll(jobs.asFlow())
    }.catch { e ->
        providerStatusProperty = JobProviderStatus.FAILED
        logger.severe { "Flow failed: ${e.message}" }
    }

    private suspend fun fetchJobs(): ForasResponse? {
        return HttpRequest.newBuilder()
            .uri(FORAS_JOB_URL.toURI())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<ForasResponse>(logger))
            ?: run {
                logger.severe { "Failed to fetch jobs returning null" }
                null
            }
    }

    private fun ForasJob.mapToJob(): Job? {
        val jobInfo = info.firstOrNull { it.language == "en" }
        val companyInfo = companyInfo.firstOrNull { it.language == "en" }
        val cityInfo = cityInfo.firstOrNull { it.language == "en" }

        if (jobInfo == null || companyInfo == null || cityInfo == null) {
            logger.warning { "Skipping job due to missing English info." }
            return null
        }

        val jobPoster = buildJobPoster(this@Foras, companyInfo.name, "Provider lacking additional company data")
        val jobURI = providerURI?.resolve("jobs/job-details/${jobInfo.opportunityId}")
            ?: run {
                logger.severe { "Failed to resolve job URI for ${jobInfo.opportunityId}" }
                return null
            }

        if (Instant.now().isAfter(endDate)) {
            logger.warning { "Skipping expired job." }
            return null
        }

        return buildJob(jobPoster, jobURI, jobInfo.name, cityInfo.name) {
            jobDescription = Jsoup.parse(jobInfo.description).text()
            jobPublishDate = LocalDate.ofInstant(createdOn, ZoneOffset.UTC)
            jobDeadline = LocalDate.ofInstant(endDate, ZoneOffset.UTC)
        }

    }

    @Serializable
    data class ForasResponse(
        val result: List<ForasJob>
    )

    @Serializable
    data class ForasJob(
        val info: List<ForasJobInfo>,
        val companyInfo: List<ForasCompanyInfo>,
        val cityInfo: List<ForasCityInfo>,
        @Contextual val createdOn: Instant,
        @Contextual val endDate: Instant
    )

    @Serializable
    data class ForasJobInfo(
        val name: String,
        val language: String,
        val description: String,
        val opportunityId: String
    )

    @Serializable
    data class ForasCompanyInfo(
        val language: String,
        val name: String
    )

    @Serializable
    data class ForasCityInfo(
        val language: String,
        val name: String
    )

    object InstantSerializer : KSerializer<Instant> {
        override val descriptor = PrimitiveSerialDescriptor(Instant::class.java.name, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
        override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
    }

    companion object {
        private const val FORAS_JOB_URL = "https://foras.ps/api/Opportunities/filteredJobs"
    }
}
