package com.jamhour.core.poster

import com.jamhour.core.job.Job
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

interface JobPoster : Comparable<JobPoster> {

    val posterName: String
    val posterLocation: String
    val posterWebsite: URI?
    val posterEstablishmentDate: LocalDate?
    val allJobsFromPoster: List<Job>
    val businessType: JobPosterBusinessType
    val posterContactInfo: JobPosterContactInfo
    val posterOverview: String
    val posterUriOnProvider: URI?
    val posterProvider: JobsProvider
    val providerVerification: JobProviderVerification
    val jobPosterBuilder: JobPosterBuilder

}
