package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.impl.JobBuilderImpl
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.buildJobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.LocalDateSerializer
import com.jamhour.util.loggerFactory
import com.jamhour.util.sendAsync
import com.jamhour.util.toBodyHandler
import com.jamhour.util.toURI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.http.HttpRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = loggerFactory(FoothillSolutions::class.java)
private const val CAREER_ENDPOINT = "https://foothillsolutions.bamboohr.com/careers"

class FoothillSolutions : AbstractJobsProvider(
    "FoothillSolutions",
    "https://www.foothillsolutions.com/".toURI()
) {
    private var providerStatus = JobProviderStatus.PROCESSING

    override fun getProviderStatus() = providerStatus

    override suspend fun getJobs(): List<Job> = coroutineScope {


        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    LocalDate::class,
                    LocalDateSerializer(FoothillJobsResponse.dateFormatter)
                )
            }
        }

        logger.info { "Initiating asynchronous request to fetch job listings from $CAREER_ENDPOINT" }

        val jobsResponse = HttpRequest.newBuilder()
            .uri("$CAREER_ENDPOINT/list".toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<FoothillJobsResponse>(logger))

        jobsResponse?.also {
            providerStatus = JobProviderStatus.ACTIVE
            logger.info { "Received job listings response for request to $CAREER_ENDPOINT" }
        } ?: run {
            providerStatus = JobProviderStatus.FAILED
            logger.severe { "Failed to fetch job listings from $CAREER_ENDPOINT. Request returned null." }
            return@coroutineScope emptyList()
        }

        logger.info { "Filtering job listings for the software department (Department ID: ${FoothillJobsResponse.SOFTWARE_DEPARTMENT_CODE})" }
        jobsResponse.result
            .filter { it.departmentId == FoothillJobsResponse.SOFTWARE_DEPARTMENT_CODE.toLong() }
            .map {
                val jobDetailsRequest = HttpRequest.newBuilder()
                    .uri(it.detailsEndPoint())
                    .GET()
                    .build()

                fetchJobDetails(it, jobDetailsRequest, jsonSerializer)
            }.awaitAll()
    }

    private fun fetchJobDetails(
        job: FoothillJob,
        jobDetailsRequest: HttpRequest,
        jsonSerializer: Json
    ): Deferred<Job> = async {
        logger.info { "Fetching job details for job ID ${job.id} from ${job.detailsEndPoint()}" }

        val jobDetails = jobDetailsRequest.sendAsync(jsonSerializer.toBodyHandler<JobDetails>(logger))

        jobDetails?.also {
            logger.info { "Successfully retrieved job details for job ID ${job.id}" }
        } ?: run {
            logger.severe { "Failed to fetch job details for job ID ${job.id}. Response returned null." }
        }

        buildJobFromDetails(job, jobDetails, getJobPoster())
    }

    private fun buildJobFromDetails(
        job: FoothillJob,
        jobDetails: JobDetails?,
        jobPoster: JobPoster
    ) = job.toJobBuilder(jobPoster).apply {
        jobDescription = jobDetails?.description ?: "No description available."
        jobPublishDate = jobDetails?.datePosted
    }.build()

    private fun getJobPoster() = buildJobPoster(this, providerName, FoothillJobsResponse.LOCATION) {
        website = providerURI
    }

    @Serializable
    private data class FoothillJobsResponse(
        val result: List<FoothillJob>
    ) {
        companion object {
            @Suppress("SpellCheckingInspection")
            const val LOCATION = "Nablus"
            const val SOFTWARE_DEPARTMENT_CODE = 18504
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
    }

    @Serializable
    private data class FoothillJob(
        val id: Long,
        val jobOpeningName: String,
        val departmentId: Long,
        val employmentStatusLabel: String,
        val location: Location
    ) {
        fun toJobBuilder(jobPoster: JobPoster) =
            JobBuilderImpl(jobPoster, "$CAREER_ENDPOINT/$id".toURI(), jobOpeningName, location.toJobLocation())

        fun detailsEndPoint() = "$CAREER_ENDPOINT/$id/detail".toURI()
    }

    @Serializable
    private data class Location(
        val city: String? = "",
        val state: String? = ""
    ) {
        fun toJobLocation() = "$state,$city"
    }

    @Serializable
    private data class JobDetails(
        val employmentStatusLabel: String = "",
        val description: String = "",
        @Contextual
        val datePosted: LocalDate? = null
    )
}
