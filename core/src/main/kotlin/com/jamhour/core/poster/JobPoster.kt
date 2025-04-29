package com.jamhour.core.poster

import com.jamhour.core.job.Job
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import com.jamhour.util.SuspendingLazyWithExpiry
import java.net.URI
import java.time.LocalDate

val jobPosterComparator: Comparator<JobPoster> =
    Comparator.comparing<JobPoster, String> { it.posterUriOnProvider?.toString() ?: "" }
        .thenComparing(JobPoster::posterName)
        .thenComparing(JobPoster::posterLocation)

interface JobPoster : Comparable<JobPoster> {

    val posterName: String
    val posterLocation: String
    val posterProvider: JobsProvider
    val cachedJobsFromPoster: SuspendingLazyWithExpiry<List<Job>>

    val businessType: JobPosterBusinessType get() = JobPosterBusinessType.OTHER
    val posterOverview: String get() = ""
    val providerVerification: JobProviderVerification get() = JobProviderVerification.NOT_SUPPORTED
    val posterUriOnProvider: URI? get() = null
    val posterWebsite: URI? get() = null
    val posterEstablishmentDate: LocalDate? get() = null
    val posterContactInfo: JobPosterContactInfo? get() = null

    override fun compareTo(other: JobPoster) = jobPosterComparator.compare(this, other)

    suspend fun getAllJobsFromProvider() = posterProvider.cachedJobs.getValue()

}
