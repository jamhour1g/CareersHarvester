package com.jamhour.core.poster

import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

interface JobPosterBuilder {

    fun name(name: String): JobPosterBuilder
    fun location(location: String): JobPosterBuilder
    fun website(website: URI): JobPosterBuilder
    fun establishmentDate(establishmentDate: LocalDate): JobPosterBuilder
    fun businessType(businessType: JobPosterBusinessType): JobPosterBuilder
    fun contactInfo(contactInfo: JobPosterContactInfo): JobPosterBuilder
    fun overview(overview: String): JobPosterBuilder
    fun uriOnProvider(uriOnProvider: URI): JobPosterBuilder
    fun provider(provider: JobsProvider): JobPosterBuilder
    fun verification(verification: JobProviderVerification): JobPosterBuilder
    fun build(): JobPoster

}