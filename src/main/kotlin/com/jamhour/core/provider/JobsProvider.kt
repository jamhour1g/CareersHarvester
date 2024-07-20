package com.jamhour.core.provider

import com.jamhour.core.job.Job
import java.net.URI

val providerComparator: Comparator<JobsProvider> =
    Comparator.comparing<JobsProvider, String> { it.providerURI?.toString() ?: "" }
        .thenComparing(JobsProvider::providerName)
        .thenComparing(JobsProvider::providerDescription)

interface JobsProvider : Comparable<JobsProvider> {

    val providerName: String
    val providerDescription: String
    val providerURI: URI?
    val providerStatus: JobProviderStatus
    val providerJobs: List<Job>

    override fun compareTo(other: JobsProvider) = providerComparator.compare(this, other)

}