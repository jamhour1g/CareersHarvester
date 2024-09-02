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


class Userpilot : AbstractJobsProvider(
    "Userpilot",
    LOCATION,
    "https://www.userpilot.com/".toURI()
) {

    override suspend fun getJobs(): List<Job> = coroutineScope {
        userpilotLogger.info { "Starting job retrieval process for $providerName from $CAREER_ENDPOINT." }

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    LocalDate::class,
                    LocalDateSerializer(UserpilotJobsResponse.dateFormatter)
                )
            }
        }

        val jobsResponse = fetchJobsList(jsonSerializer)

        if (jobsResponse == null) {
            userpilotLogger.warning { "Job retrieval failed. Setting provider status to FAILED." }
            providerStatusProperty = JobProviderStatus.FAILED
            return@coroutineScope emptyList()
        }

        userpilotLogger.info { "Job retrieval successful. Setting provider status to ACTIVE." }
        providerStatusProperty = JobProviderStatus.ACTIVE
        filterAndFetchJobDetails(jobsResponse, jsonSerializer)
    }

    private suspend fun fetchJobsList(jsonSerializer: Json): UserpilotJobsResponse? {
        userpilotLogger.info { "Initiating asynchronous request to fetch job listings from $CAREER_ENDPOINT." }

        val jobsResponse = HttpRequest.newBuilder()
            .uri(CAREER_ENDPOINT.toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<UserpilotJobsResponse>(userpilotLogger))

        if (jobsResponse == null) {
            userpilotLogger.severe { "Failed to fetch job listings from $CAREER_ENDPOINT. Received a null response." }
            return null
        }

        userpilotLogger.info { "Successfully received job listings response from $CAREER_ENDPOINT." }
        return jobsResponse
    }

    private suspend fun filterAndFetchJobDetails(
        jobsResponse: UserpilotJobsResponse,
        jsonSerializer: Json
    ): List<Job> {
        userpilotLogger.info { "Filtering job listings for the software department (Department ID: ${UserpilotJobsResponse.SOFTWARE_DEPARTMENT_CODE}). Total jobs retrieved: ${jobsResponse.result.size}" }

        val filteredJobs = jobsResponse.result.filter {
            val isInPalestine = it.location.toJobLocation() == LOCATION
            val isInSoftware = it.departmentId == UserpilotJobsResponse.SOFTWARE_DEPARTMENT_CODE.toLong()
            (isInSoftware && isInPalestine) || (isInSoftware && it.isRemote == true)
        }

        if (filteredJobs.isEmpty()) {
            userpilotLogger.warning { "No job listings found for the software department (Department ID: ${UserpilotJobsResponse.SOFTWARE_DEPARTMENT_CODE})." }
            return emptyList()
        }

        userpilotLogger.info { "Found ${filteredJobs.size} job(s) in the software department. Fetching details for each job." }
        return filteredJobs.map { it.fetchJobDetails(userpilotLogger, getDefaultJobPoster(), jsonSerializer) }
            .awaitAll()
    }

    companion object {
        private val userpilotLogger = loggerFactory(Userpilot::class.java)
        private const val LOCATION = "Ramallah,West Bank"
        private const val CAREER_ENDPOINT = "https://userpilot.bamboohr.com/careers/list"
    }
}

@Serializable
private data class UserpilotJobsResponse(
    val result: List<UserpilotJob>
) {
    companion object {
        const val SOFTWARE_DEPARTMENT_CODE = 18573
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

@Serializable
private data class UserpilotJob(
    val id: Long,
    val jobOpeningName: String,
    val departmentId: Long,
    val employmentStatusLabel: String,
    val isRemote: Boolean?,
    val location: UserpilotLocation
) {
    @Transient
    private val careerURI = "https://userpilot.bamboohr.com/careers/$id".toURI()

    @Transient
    private val careerDetailsEndpointURI = "https://userpilot.bamboohr.com/careers/$id/detail".toURI()

    fun toJob(
        jobPoster: JobPoster,
        foothillJobDetails: UserpilotJobDetails?
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

            val jobDetails =
                jobDetailsRequest.sendAsync(stringFormat.toBodyHandler<UserpilotJobDetails>(logger))

            if (jobDetails == null) {
                logger.severe { "Failed to fetch job details for job ID $id. Response returned null." }
            } else {
                logger.info { "Successfully retrieved job details for job ID $id" }
            }

            toJob(jobPoster, jobDetails)
        }
    }
}


@Serializable
private data class UserpilotLocation(
    val city: String? = "",
    val state: String? = ""
) {
    fun toJobLocation() = "$state,$city"
}

@Serializable
private data class UserpilotJobDetails(
    val employmentStatusLabel: String = "",
    val description: String = "",
    @Contextual
    val datePosted: LocalDate? = null
)
