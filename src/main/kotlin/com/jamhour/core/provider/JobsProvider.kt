package com.jamhour.core.provider

import com.jamhour.core.job.Job
import com.jamhour.util.SuspendingLazyWithExpiry
import kotlinx.coroutines.flow.Flow
import java.net.URI

val providerComparator: Comparator<JobsProvider> =
    Comparator.comparing<JobsProvider, String> { it.providerURI?.toString() ?: "" }
        .thenComparing(JobsProvider::providerName)
        .thenComparing(JobsProvider::providerDescription)

interface JobsProvider : Comparable<JobsProvider> {

    val providerName: String
    val location: String
    val cachedJobs: SuspendingLazyWithExpiry<List<Job>>

    val providerDescription: String get() = ""
    val providerURI: URI? get() = null

    override fun compareTo(other: JobsProvider) = providerComparator.compare(this, other)

    fun getProviderStatus(): JobProviderStatus
    fun getJobs(): Flow<Job>

}