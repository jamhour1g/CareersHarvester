package com.jamhour.core.provider

import com.jamhour.core.job.Job
import com.jamhour.core.poster.buildJobPoster
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.net.URI
import java.util.Objects

abstract class AbstractJobsProvider(
    override val providerName: String,
    override val location: String,
    override val providerURI: URI? = null,
) : JobsProvider {

    // ! TODO: find a way to invalidate this cashed data
    override val cachedJobs: Deferred<List<Job>> by lazy { async { getJobs() } }
    var providerStatusProperty = JobProviderStatus.PROCESSING; protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractJobsProvider) return false
        return providerName == other.providerName &&
                providerDescription == other.providerDescription &&
                providerURI == other.providerURI
    }

    fun getDefaultJobPoster() = buildJobPoster(this, providerName, location) {
        website = providerURI
    }

    override fun getProviderStatus() = providerStatusProperty
    override fun hashCode() = Objects.hash(providerName, providerDescription, providerURI)
    override fun toString() =
        "AbstractJobsProvider(providerName='$providerName', providerDescription='$providerDescription', providerURI=$providerURI, providerStatus=${getProviderStatus()})"
}