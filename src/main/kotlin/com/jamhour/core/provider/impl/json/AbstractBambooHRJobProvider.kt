package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.impl.JobBuilderImpl
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.LocalDateSerializer
import com.jamhour.util.sendAsync
import com.jamhour.util.toBodyHandler
import com.jamhour.util.toURI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import kotlin.time.Duration

abstract class AbstractBambooHRJobProvider(
    override val providerName: String,
    override val location: String,
    override val providerURI: URI,
    val careerPageLink: URI,
    expiryDuration: Duration
) : AbstractJobsProvider(providerName, location, providerURI, expiryDuration) {

    private val jobListEndpoint: URI = "$careerPageLink/list".toURI()

    init {
        require(careerPageLink.toString().contains("bamboohr.com")) {
            "The provided career page link is not a valid BambooHR URL."
        }
    }

    /**
     * Override this function to implement specific job filtering logic.
     */
    internal open fun filterJob(job: BambooHRJob): Boolean = true

    override fun getJobs(): Flow<Job> = flow {
        logger.info { "Starting job retrieval for $providerName from $careerPageLink." }

        val jsonParser = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(LocalDate::class, LocalDateSerializer(BambooHRJobsResponse.dateFormatter))
            }
        }

        val jobResponse = fetchJobList(jsonParser)
            ?: throw IllegalStateException("Failed to retrieve job listings.")

        providerStatusProperty = JobProviderStatus.ACTIVE

        emitAll(fetchFilteredJobDetails(jobResponse, jsonParser))
    }.catch { e ->
        providerStatusProperty = JobProviderStatus.FAILED
        logger.severe { "Exception during job retrieval: ${e.message}" }
    }


    private fun StringFormat.toJobsResponseBodyHandler(careerPageLink: URI) = HttpResponse.BodyHandler {
        HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofString(Charsets.UTF_8)
        ) { responseString ->
            val updatedResponse = responseString.replace(
                Regex("\"locationType\":\\s*\"\\w+\""),
                "\"bambooHRPageLink\":\"$careerPageLink\""
            )
            logger.info { "Received response from BambooHR API." }

            runCatching {
                logger.info { "Parsing response to ${BambooHRJobsResponse::class.simpleName}." }
                decodeFromString<BambooHRJobsResponse>(updatedResponse)
            }.onFailure {
                logger.severe {
                    """
                    Error parsing response $updatedResponse
                    Exception: ${it.stackTraceToString()}
                    """.trimIndent()
                }
            }.getOrNull()
        }
    }

    private suspend fun fetchJobList(jsonParser: StringFormat): BambooHRJobsResponse? {
        logger.info { "Sending asynchronous request to fetch job listings from $jobListEndpoint." }

        val jobResponse = HttpRequest.newBuilder()
            .uri(jobListEndpoint)
            .GET()
            .build()
            .sendAsync(jsonParser.toJobsResponseBodyHandler(careerPageLink))

        if (jobResponse == null) {
            logger.severe { "Failed to fetch job listings from $jobListEndpoint. Response was null." }
            return null
        }

        logger.info { "Successfully received job listings response from $jobListEndpoint." }
        return jobResponse
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchFilteredJobDetails(
        jobsResponse: BambooHRJobsResponse,
        jsonParser: StringFormat,
    ): Flow<Job> {
        logger.info { "Filtering job listings. Total jobs retrieved: ${jobsResponse.result.size}." }

        val filteredJobs = jobsResponse.result.filter { filterJob(it) }

        if (filteredJobs.isEmpty()) {
            logger.warning { "No job listings matched the filter criteria." }
            return emptyFlow()
        }

        logger.info { "Found ${filteredJobs.size} job(s) after filtering. Fetching details for each job." }
        return filteredJobs.asFlow()
            .flatMapMerge { job ->
                flowOf(job.retrieveJobDetails(logger, getDefaultJobPoster(), jsonParser))
            }

    }
}

@Serializable
private data class BambooHRJobsResponse(
    val result: List<BambooHRJob>
) {
    companion object {
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

@Serializable
internal data class BambooHRJob(
    val bambooHRPageLink: String,
    val id: Long,
    val jobOpeningName: String,
    val departmentId: Int?,
    val employmentStatusLabel: String,
    val isRemote: Boolean?,
    val location: BambooHRLocation
) {

    val jobDetailURI: URI by lazy { "$bambooHRPageLink/$id".toURI() }
    val jobDetailsEndpointURI: URI by lazy { "$jobDetailURI/detail".toURI() }

    fun toJob(
        jobPoster: JobPoster,
        jobDetails: BambooHRJobDetails?
    ): Job = JobBuilderImpl(
        jobPoster,
        jobDetailURI,
        jobOpeningName,
        location.toJobLocation()
    ).apply {
        jobDescription = jobDetails?.description ?: "No description available."
        jobPublishDate = jobDetails?.datePosted
    }.build()

    suspend fun retrieveJobDetails(
        logger: Logger,
        jobPoster: JobPoster,
        jsonParser: StringFormat
    ): Job = coroutineScope {
        logger.info { "Fetching job details for job ID $id from $jobDetailsEndpointURI." }

        val jobDetailsRequest = HttpRequest.newBuilder()
            .uri(jobDetailsEndpointURI)
            .GET()
            .build()

        val jobDetailsResponse = jobDetailsRequest.sendAsync(
            jsonParser.toBodyHandler<BambooHRJobDetails>(logger)
        )

        if (jobDetailsResponse == null) {
            logger.severe { "Failed to fetch job details for job ID $id. Response was null." }
        } else {
            logger.info { "Successfully retrieved job details for job ID $id." }
        }

        toJob(jobPoster, jobDetailsResponse)
    }
}

@Serializable
internal data class BambooHRLocation(
    val city: String? = "",
    val state: String? = ""
) {
    fun toJobLocation(): String = when {
        city != null && state != null -> "$city, $state"
        city == null && state == null -> "Remote"
        else -> "Not available"
    }
}

@Serializable
internal data class BambooHRJobDetails(
    val employmentStatusLabel: String = "",
    val description: String = "",
    @Contextual
    val datePosted: LocalDate? = null
)
