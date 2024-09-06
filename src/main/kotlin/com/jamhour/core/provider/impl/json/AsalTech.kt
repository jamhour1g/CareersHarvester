package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.buildJob
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.URISerializer
import com.jamhour.util.ZonedDateTimeSerializer
import com.jamhour.util.sendAsync
import com.jamhour.util.toBodyHandler
import com.jamhour.util.toURI
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.http.HttpRequest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AsalTech : AbstractJobsProvider(
    "AsalTech",
    LOCATION,
    "https://www.asaltech.com/".toURI()
) {

    override suspend fun getJobs(): List<Job> = coroutineScope {
        logger.info { "Starting job retrieval process for $providerName from $JOBS_API_ENDPOINT." }

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    ZonedDateTime::class,
                    ZonedDateTimeSerializer(AsalJobsResponse.DATE_TIME_FORMAT)
                )
            }
        }

        logger.info { "Sending HTTP request to fetch job listings from $JOBS_API_ENDPOINT." }

        val response = HttpRequest.newBuilder()
            .uri(JOBS_API_ENDPOINT.toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<AsalJobsResponse>(logger))

        if (response == null) {
            logger.severe { "Failed to receive job listings from $JOBS_API_ENDPOINT. The response was null." }
            providerStatusProperty = JobProviderStatus.FAILED
            return@coroutineScope emptyList()
        }

        logger.info { "Successfully received job listings from $JOBS_API_ENDPOINT. Number of jobs found: ${response.offers.size}" }
        providerStatusProperty = JobProviderStatus.ACTIVE

        val filteredJobs = response.offers.filter { it.categoryCode == AsalJobsResponse.IT_CATEGORY_CODE }

        if (filteredJobs.isEmpty()) {
            logger.warning { "No IT category jobs found in the response from $JOBS_API_ENDPOINT." }
        } else {
            logger.info { "Filtering complete. ${filteredJobs.size} IT job(s) found." }
        }

        filteredJobs.map { it.toJob(getDefaultJobPoster()) }
    }

    companion object {
        private const val LOCATION = "Ramallah,rawabi"
        private const val JOBS_API_ENDPOINT = "https://career.recruitee.com/api/c/40756/widget"
    }
}

@Serializable
private data class AsalJobsResponse(val offers: List<AsalJob>) {
    companion object {
        const val IT_CATEGORY_CODE = "information_technology"
        val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
    }
}

// TODO : add default values and allow for coerce null values in the json builder
@Serializable
private data class AsalJob(
    @SerialName("published_at") @Contextual val publishDate: ZonedDateTime,
    val title: String,
    val location: String,
    @SerialName("category_code") val categoryCode: String,
    val requirements: String?,
    @SerialName("careers_url") @Serializable(with = URISerializer::class) val jobUri: URI,
    val description: String
) {
    fun toJob(jobPoster: JobPoster) = buildJob(jobPoster, jobUri, title, location) {
        jobDescription = description
        jobRequirements = requirements ?: ""
        jobPublishDate = publishDate.toLocalDate()
    }
}