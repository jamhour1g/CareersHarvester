package com.jamhour.core.provider.impl.json

import com.jamhour.core.job.Job
import com.jamhour.core.job.buildJob
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.buildJobPoster
import com.jamhour.core.provider.AbstractJobsProvider
import com.jamhour.core.provider.JobProviderStatus
import com.jamhour.util.URISerializer
import com.jamhour.util.ZonedDateTimeSerializer
import com.jamhour.util.loggerFactory
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

private val logger = loggerFactory(AsalTech::class.java)

class AsalTech : AbstractJobsProvider(
    "AsalTech",
    "https://www.asaltech.com/".toURI()
) {

    private var providerStatus = JobProviderStatus.PROCESSING
    override fun getProviderStatus() = providerStatus

    override suspend fun getJobs(): List<Job> = coroutineScope {

        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(
                    ZonedDateTime::class,
                    ZonedDateTimeSerializer(AsalJobsResponse.DATE_TIME_FORMAT)
                )
            }
        }

        logger.info { "Sending HTTP request to fetch job listings from ${AsalJobsResponse.API_URI}" }

        val response = HttpRequest.newBuilder()
            .uri(AsalJobsResponse.API_URI.toURI())
            .GET()
            .build()
            .sendAsync(jsonSerializer.toBodyHandler<AsalJobsResponse>(logger))

        response?.let {
            providerStatus = JobProviderStatus.ACTIVE
            logger.info { "Successfully received job listings from ${AsalJobsResponse.API_URI}. Number of jobs found: ${it.offers.size}" }

            it.offers
                .filter { it.categoryCode == AsalJobsResponse.IT_CATEGORY_CODE }
                .map { it.toJob(getJobPoster()) }

        } ?: run {
            logger.severe { "Failed to receive job listings from ${AsalJobsResponse.API_URI}. The response was null." }
            providerStatus = JobProviderStatus.FAILED

            emptyList()
        }
    }

    fun getJobPoster() = buildJobPoster(this, providerName, AsalJobsResponse.LOCATION) {
        website = providerURI
    }

    @Serializable
    private data class AsalJobsResponse(val offers: List<AsalJob>) {
        companion object {
            @Suppress("SpellCheckingInspection")
            const val LOCATION = "Ramallah,rawabi"
            const val IT_CATEGORY_CODE = "information_technology"
            val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            const val API_URI = "https://career.recruitee.com/api/c/40756/widget"
        }
    }

    @Serializable
    private data class AsalJob(
        @SerialName("published_at") @Contextual val publishDate: ZonedDateTime,
        val title: String,
        val location: String,
        @SerialName("category_code") val categoryCode: String,
        val requirements: String,
        @SerialName("careers_url") @Serializable(with = URISerializer::class) val jobUri: URI,
        val description: String
    ) {
        fun toJob(jobPoster: JobPoster) = buildJob(jobPoster, jobUri, title, location) {
            jobDescription = description
            jobRequirements = requirements
            jobPublishDate = publishDate.toLocalDate()
        }
    }
}
