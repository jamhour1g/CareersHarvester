package com.jamhour.core.provider

import com.jamhour.core.job.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import java.net.URI
import kotlin.coroutines.CoroutineContext

val providerComparator: Comparator<JobsProvider> =
    Comparator.comparing<JobsProvider, String> { it.providerURI?.toString() ?: "" }
        .thenComparing(JobsProvider::providerName)
        .thenComparing(JobsProvider::providerDescription)

interface JobsProvider : Comparable<JobsProvider>, CoroutineScope {

    val providerName: String
    val cachedJobs: Deferred<List<Job>>

    val providerDescription: String get() = ""
    val providerURI: URI? get() = null

    override val coroutineContext: CoroutineContext get() = Dispatchers.Default + kotlinx.coroutines.Job()
    override fun compareTo(other: JobsProvider) = providerComparator.compare(this, other)

    fun getProviderStatus(): JobProviderStatus
    suspend fun getJobs(): List<Job>


}