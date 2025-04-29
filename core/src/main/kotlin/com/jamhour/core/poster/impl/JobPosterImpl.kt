package com.jamhour.core.poster.impl

import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.JobPosterBuilder
import com.jamhour.core.poster.JobPosterBusinessType
import com.jamhour.core.poster.JobPosterContactInfo
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import com.jamhour.util.SuspendingLazyWithExpiry
import java.net.URI
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

data class JobPosterImpl(
    override val posterName: String,
    override val posterLocation: String,
    override val posterWebsite: URI?,
    override val posterEstablishmentDate: LocalDate?,
    override val businessType: JobPosterBusinessType,
    override val posterContactInfo: JobPosterContactInfo?,
    override val posterOverview: String,
    override val posterUriOnProvider: URI?,
    override val posterProvider: JobsProvider,
    override val providerVerification: JobProviderVerification,
) : JobPoster {
    override val cachedJobsFromPoster = SuspendingLazyWithExpiry(10.minutes) {
        getAllJobsFromProvider().filter { it.jobPoster == this }
    }
}

fun buildJobPoster(
    provider: JobsProvider,
    name: String,
    location: String,
    builderAction: (JobPosterBuilder.() -> Unit)? = null
): JobPoster {

    val jobPosterBuilderImpl = JobPosterBuilderImpl(
        provider = provider,
        name = name,
        location = location
    )

    builderAction?.let { jobPosterBuilderImpl.apply { it() } }

    return jobPosterBuilderImpl.build()
}