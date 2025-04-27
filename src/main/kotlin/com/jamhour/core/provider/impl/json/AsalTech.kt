package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.buildJob
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.http.HttpRequest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

class AsalTech() : AbstractJobsProvider(
    "AsalTech",
    LOCATION,
    "https://www.asaltech.com/".toURI(),
    1.minutes
) {

    override fun getJobs(): Flow<Job> = flow {
        logger.info { "Fetching jobs from $JOBS_API_ENDPOINT." }

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    ZonedDateTime::class,
                    ZonedDateTimeSerializer(AsalJobsResponse.DATE_TIME_FORMAT)
                )
            }
        }

        val response = HttpRequest.newBuilder()
            .uri(JOBS_API_ENDPOINT.toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<AsalJobsResponse>(logger))
            ?: throw IllegalStateException("Null response from $JOBS_API_ENDPOINT")

        providerStatusProperty = JobProviderStatus.ACTIVE

        val cutoffDate = ZonedDateTime.now().minusMonths(2)
        val filteredJobs = response.offers.filter {
            it.categoryCode == AsalJobsResponse.IT_CATEGORY_CODE &&
                    it.publishDate.isAfter(cutoffDate)
        }

        if (filteredJobs.isEmpty()) {
            logger.warning { "No recent IT jobs found at $JOBS_API_ENDPOINT." }
        } else {
            logger.info { "Found ${filteredJobs.size} recent IT jobs." }
        }

        emitAll(filteredJobs.map { it.toJob(getDefaultJobPoster()) }.asFlow())

    }.catch { e ->
        providerStatusProperty = JobProviderStatus.FAILED
        logger.severe { "Job retrieval error: ${e.message}" }
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
    fun toJob(jobPoster: JobPoster): Job {
        val reversedLocation = location.split(", ").asReversed().joinToString()
        return buildJob(jobPoster, jobUri, title, reversedLocation) {
            jobDescription = description
            jobRequirements = requirements ?: ""
            jobPublishDate = publishDate.toLocalDate()
        }
    }
}