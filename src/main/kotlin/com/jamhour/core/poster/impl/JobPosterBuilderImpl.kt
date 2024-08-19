package com.jamhour.core.poster.impl

import com.jamhour.core.poster.JobPosterBuilder
import com.jamhour.core.poster.JobPosterBusinessType
import com.jamhour.core.poster.JobPosterContactInfo
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

class JobPosterBuilderImpl(override val provider: JobsProvider) : JobPosterBuilder {

    override var name: String = ""
    override var location: String = ""
    override var overview: String = ""
    override var businessType: JobPosterBusinessType = JobPosterBusinessType.OTHER
    override var verification: JobProviderVerification = JobProviderVerification.NOT_SUPPORTED

    override var website: URI? = null
    override var establishmentDate: LocalDate? = null
    override var contactInfo: JobPosterContactInfo? = null
    override var uriOnProvider: URI? = null

    override fun build() = JobPosterImpl(
        name,
        location,
        website,
        establishmentDate,
        businessType,
        contactInfo,
        overview,
        uriOnProvider,
        provider,
        verification
    )

}