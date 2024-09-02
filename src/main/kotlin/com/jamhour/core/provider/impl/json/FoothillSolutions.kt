package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.impl.JobBuilderImpl
import com.jamhour.core.poster.JobPoster
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
import kotlinx.serialization.StringFormat
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.http.HttpRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

class FoothillSolutions : AbstractJobsProvider(
    "FoothillSolutions",
    LOCATION,
    "https://www.foothillsolutions.com/".toURI()
) {

    override suspend fun getJobs(): List<Job> = coroutineScope {
        foothillLogger.info { "Starting job retrieval process for $providerName from $CAREER_ENDPOINT." }

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    LocalDate::class,
                    LocalDateSerializer(FoothillJobsResponse.dateFormatter)
                )
            }
        }

        val jobsResponse = fetchJobsList(jsonSerializer)

        if (jobsResponse == null) {
            foothillLogger.warning { "Job retrieval failed. Setting provider status to FAILED." }
            providerStatusProperty = JobProviderStatus.FAILED
            return@coroutineScope emptyList()
        }

        foothillLogger.info { "Job retrieval successful. Setting provider status to ACTIVE." }
        providerStatusProperty = JobProviderStatus.ACTIVE
        filterAndFetchJobDetails(jobsResponse, jsonSerializer)
    }

    private suspend fun fetchJobsList(jsonSerializer: Json): FoothillJobsResponse? {
        foothillLogger.info { "Initiating asynchronous request to fetch job listings from $CAREER_ENDPOINT." }

        val foothillJobsResponse = HttpRequest.newBuilder()
            .uri("$CAREER_ENDPOINT/list".toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<FoothillJobsResponse>(foothillLogger))

        if (foothillJobsResponse == null) {
            foothillLogger.severe { "Failed to fetch job listings from $CAREER_ENDPOINT. Received a null response." }
            return null
        }

        foothillLogger.info { "Successfully received job listings response from $CAREER_ENDPOINT." }
        return foothillJobsResponse
    }

    private suspend fun filterAndFetchJobDetails(
        jobsResponse: FoothillJobsResponse,
        jsonSerializer: Json
    ): List<Job> {
        foothillLogger.info { "Filtering job listings for the software department (Department ID: ${FoothillJobsResponse.SOFTWARE_DEPARTMENT_CODE}). Total jobs retrieved: ${jobsResponse.result.size}" }

        val filteredJobs = jobsResponse.result.filter {
            it.departmentId == FoothillJobsResponse.SOFTWARE_DEPARTMENT_CODE.toLong()
        }

        if (filteredJobs.isEmpty()) {
            foothillLogger.warning { "No job listings found for the software department (Department ID: ${FoothillJobsResponse.SOFTWARE_DEPARTMENT_CODE})." }
            return emptyList()
        }

        foothillLogger.info { "Found ${filteredJobs.size} job(s) in the software department. Fetching details for each job." }
        return filteredJobs.map { it.fetchJobDetails(foothillLogger, getDefaultJobPoster(), jsonSerializer) }.awaitAll()
    }

    companion object {
        private val foothillLogger = loggerFactory(FoothillSolutions::class.java)
        private const val LOCATION = "Nablus"
        private const val CAREER_ENDPOINT = "https://foothillsolutions.bamboohr.com/careers"
    }
}

@Serializable
private data class FoothillJobsResponse(
    val result: List<FoothillJob>
) {
    companion object {
        const val SOFTWARE_DEPARTMENT_CODE = 18504
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
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
    @Transient
    private val careerURI = "https://foothillsolutions.bamboohr.com/careers/$id".toURI()

    @Transient
    private val careerDetailsEndpointURI = "https://foothillsolutions.bamboohr.com/careers/$id/detail".toURI()

    fun toJob(
        jobPoster: JobPoster,
        foothillJobDetails: FoothillJobDetails?
    ) = JobBuilderImpl(jobPoster, careerURI, jobOpeningName, location.toJobLocation()).apply {
        jobDescription = foothillJobDetails?.description ?: "No description available."
        jobPublishDate = foothillJobDetails?.datePosted
    }.build()

    suspend fun fetchJobDetails(
        logger: Logger,
        jobPoster: JobPoster,
        stringFormat: StringFormat
    ): Deferred<Job> = coroutineScope {
        async {
            logger.info { "Fetching job details for job ID $id" }

            val jobDetailsRequest = HttpRequest.newBuilder()
                .uri(careerDetailsEndpointURI)
                .GET()
                .build()

            logger.info { "Fetching job details for job ID $id from $careerDetailsEndpointURI" }

            val foothillJobDetails = jobDetailsRequest.sendAsync(stringFormat.toBodyHandler<FoothillJobDetails>(logger))

            if (foothillJobDetails == null) {
                logger.severe { "Failed to fetch job details for job ID $id. Response returned null." }
            } else {
                logger.info { "Successfully retrieved job details for job ID $id" }
            }

            toJob(jobPoster, foothillJobDetails)
        }
    }
}


@Serializable
private data class Location(
    val city: String? = "",
    val state: String? = ""
) {
    fun toJobLocation() = "$state,$city"
}

@Serializable
private data class FoothillJobDetails(
    val employmentStatusLabel: String = "",
    val description: String = "",
    @Contextual
    val datePosted: LocalDate? = null
)
