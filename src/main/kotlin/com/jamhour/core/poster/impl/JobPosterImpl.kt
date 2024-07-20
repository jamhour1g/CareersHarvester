package com.jamhour.core.poster.impl

import com.jamhour.core.job.Job
import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.JobPosterBusinessType
import com.jamhour.core.poster.JobPosterContactInfo
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

@JvmRecord
data class JobPosterImpl(
    override val posterName: String,
    override val posterLocation: String,
    override val posterWebsite: URI?,
    override val posterEstablishmentDate: LocalDate?,
    override val allJobsFromPoster: List<Job>,
    override val businessType: JobPosterBusinessType,
    override val posterContactInfo: JobPosterContactInfo,
    override val posterOverview: String,
    override val posterUriOnProvider: URI?,
    override val posterProvider: JobsProvider,
    override val providerVerification: JobProviderVerification,
) : JobPoster
