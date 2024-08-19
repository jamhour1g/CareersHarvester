package com.jamhour.core.poster

import com.jamhour.core.poster.impl.JobPosterBuilderImpl
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

interface JobPosterBuilder {

    val provider: JobsProvider

    var name: String
    var location: String
    var overview: String
    var businessType: JobPosterBusinessType
    var verification: JobProviderVerification
    var website: URI?
    var establishmentDate: LocalDate?
    var contactInfo: JobPosterContactInfo?
    var uriOnProvider: URI?

    fun build(): JobPoster

}

fun buildJobPoster(
    provider: JobsProvider,
    builderAction: JobPosterBuilder.() -> Unit
): JobPoster = JobPosterBuilderImpl(provider).apply(builderAction).build()