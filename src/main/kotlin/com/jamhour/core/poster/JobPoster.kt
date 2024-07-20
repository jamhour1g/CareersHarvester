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
    val allJobsFromPoster: List<Job>
    val businessType: JobPosterBusinessType
    val posterOverview: String
    val posterProvider: JobsProvider
    val providerVerification: JobProviderVerification
    val posterUriOnProvider: URI?
    val posterWebsite: URI?
    val posterEstablishmentDate: LocalDate?
    val posterContactInfo: JobPosterContactInfo?

    override fun compareTo(other: JobPoster): Int {
        return jobPosterComparator.compare(this, other)
    }

    fun getAllJobsFromProvider() = posterProvider.providerJobs
        .asSequence()
        .filter { it.jobPoster == this }
        .toList()

}
