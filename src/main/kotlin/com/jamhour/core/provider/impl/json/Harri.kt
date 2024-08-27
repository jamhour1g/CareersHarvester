package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.impl.JobBuilderImpl
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.buildJobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.ZonedDateTimeSerializer
import com.jamhour.util.loggerFactory
import com.jamhour.util.sendAsync
import com.jamhour.util.toBodyHandler
import com.jamhour.util.toURI
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.http.HttpRequest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

class Harri : AbstractJobsProvider(
    "Harri",
    "https://harri.com/".toURI()
) {

    override suspend fun getJobs(): List<Job> = coroutineScope {
        harriLogger.info { "Starting job retrieval process for $providerName from $JOBS_API_ENDPOINT." }

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    ZonedDateTime::class,
                    ZonedDateTimeSerializer(JobsResponse.DATE_TIME_FORMAT)
                )
            }
        }

        harriLogger.info { "Sending HTTP request to fetch job listings from $JOBS_API_ENDPOINT." }

        val harriJobsFormat = HttpRequest.newBuilder()
            .uri(JOBS_API_ENDPOINT.toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<JobsFormat>(harriLogger))


        if (harriJobsFormat == null) {
            harriLogger.severe { "Failed to receive job listings from $JOBS_API_ENDPOINT. The response was null." }
            providerStatusProperty = JobProviderStatus.FAILED
            return@coroutineScope emptyList()
        }

        providerStatusProperty = JobProviderStatus.ACTIVE

        val jobs = harriJobsFormat.data.harriJobs
        harriLogger.info { "Successfully received job listings from $JOBS_API_ENDPOINT. Number of jobs found: ${jobs.size}" }

        if (jobs.isEmpty()) {
            harriLogger.warning { "No jobs were found in the response from $JOBS_API_ENDPOINT." }
            return@coroutineScope emptyList()
        }

        harriLogger.info { "Processing and fetching details for each job." }
        harriJobsFormat.data.harriJobs
            .map { it.getJobDetails(harriLogger, getJobPoster(), jsonSerializer) }
    }

    private fun getJobPoster() = buildJobPoster(this, providerName, LOCATION) {
        website = "https://harri.com/".toURI()
    }

    companion object {
        private val harriLogger = loggerFactory(Harri::class.java)
        private const val JOBS_API_ENDPOINT = "https://gateway.harri.com/core-reader/api/v1/profile/brand/646003"
        private const val LOCATION = "Palestine, Ramallah, Sateh Marhaba, Al-bireh"
    }
}

@Serializable
private data class JobsFormat(val data: JobsResponse)

@Serializable
private data class JobsResponse(@SerialName("Jobs") val harriJobs: List<HarriJob>) {
    companion object {
        val DATE_TIME_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME
    }
}

@Serializable
private data class HarriJob(@SerialName("Job") val harriJobDetails: HarriJobDetails) {
    fun getJobBuilder(jobPoster: JobPoster) =
        JobBuilderImpl(jobPoster, harriJobDetails.jobURI, harriJobDetails.title, harriJobDetails.location)

    suspend fun getJobDetails(
        logger: Logger,
        jobPoster: JobPoster,
        stringFormat: StringFormat,
    ): Job = coroutineScope {
        logger.info { "Fetching job details from ${harriJobDetails.jobDetailsEndPointURI}." }

        val jobDetailsFormat = HttpRequest.newBuilder()
            .uri(harriJobDetails.jobDetailsEndPointURI)
            .GET()
            .build()
            .sendAsync(stringFormat.toBodyHandler<JobDetailsFormat>(logger))
            .also {
                logger.info { "Successfully fetched job details for endpoint: ${harriJobDetails.jobDetailsEndPointURI}." }
            }

        if (jobDetailsFormat == null) {
            logger.warning { "Received null response when fetching job details from ${harriJobDetails.jobDetailsEndPointURI}. Defaulting to an empty description." }
        }

        getJobBuilder(jobPoster)
            .apply {
                jobDescription = jobDetailsFormat?.data?.description ?: ""
                logger.info { "Job description for ${harriJobDetails.jobDetailsEndPointURI} set successfully." }
            }
            .build()
    }

}

@Serializable
private data class HarriJobDetails(
    @SerialName("publish_date") @Contextual val publishDate: ZonedDateTime,
    @SerialName("end_date") @Contextual val deadLine: ZonedDateTime,
    @SerialName("alias_position") val title: String,
    @Transient @Suppress("SpellCheckingInspection") val location: String = "Palestine, Ramallah, Sateh Marhaba, Al-bireh",
    val id: Long,
) {
    @Transient
    val jobURI =
        "https://harri.com/careers_palestine/job/$id-${title.lowercase().replace(" ", "-")}".toURI()

    @Transient
    val jobDetailsEndPointURI = "https://gateway.harri.com/core-reader/api/v1/profile/job/$id".toURI()
}

@Serializable
private class JobDetailsFormat(val data: JobDescription)

@Serializable
private class JobDescription(val description: String = "")

