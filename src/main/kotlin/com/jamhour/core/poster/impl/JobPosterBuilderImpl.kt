package com.jamhour.core.poster.impl

import com.jamhour.core.poster.JobPosterBuilder
import com.jamhour.core.poster.JobPosterBusinessType
import com.jamhour.core.poster.JobPosterContactInfo
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

class JobPosterBuilderImpl : JobPosterBuilder {

    private var name: String = ""
    private var location: String = ""
    private var overview: String = ""
    private var businessType: JobPosterBusinessType = JobPosterBusinessType.OTHER
    private var verification: JobProviderVerification = JobProviderVerification.NOT_SUPPORTED
    private var website: URI? = null
    private var establishmentDate: LocalDate? = null
    private var contactInfo: JobPosterContactInfo? = null
    private var uriOnProvider: URI? = null
    private var provider: JobsProvider? = null

    override fun name(name: String) = apply { this.name = name }
    override fun location(location: String) = apply { this.location = location }
    override fun website(website: URI) = apply { this.website = website }
    override fun establishmentDate(establishmentDate: LocalDate) = apply { this.establishmentDate = establishmentDate }
    override fun businessType(businessType: JobPosterBusinessType) = apply { this.businessType = businessType }
    override fun contactInfo(contactInfo: JobPosterContactInfo) = apply { this.contactInfo = contactInfo }
    override fun overview(overview: String) = apply { this.overview = overview }
    override fun uriOnProvider(uriOnProvider: URI) = apply { this.uriOnProvider = uriOnProvider }
    override fun provider(provider: JobsProvider) = apply { this.provider = provider }
    override fun verification(verification: JobProviderVerification) = apply { this.verification = verification }
    override fun build() = JobPosterImpl(
        name,
        location,
        website,
        establishmentDate,
        businessType,
        contactInfo,
        overview,
        uriOnProvider,
        checkNotNull(provider) { "JobsProvider cannot be null" },
        verification
    )
}