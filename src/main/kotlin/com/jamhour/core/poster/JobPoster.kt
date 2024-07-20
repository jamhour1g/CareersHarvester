package com.jamhour.core.poster

import com.jamhour.core.job.Job
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

val jobPosterComparator: Comparator<JobPoster> = Comparator.comparing<JobPoster, String> {
    it.posterUriOnProvider
        ?.toString() ?: ""
}.thenComparing(JobPoster::posterName)
    .thenComparing(JobPoster::posterLocation)

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

    override fun compareTo(other: JobPoster): Int {
        return jobPosterComparator.compare(this, other)
    }

}
