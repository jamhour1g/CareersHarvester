package com.jamhour.core.provider

import com.jamhour.core.poster.impl.buildJobPoster
import com.jamhour.util.SuspendingLazyWithExpiry
import com.jamhour.util.loggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import java.net.URI
import java.util.*
import kotlin.time.Duration

abstract class AbstractJobsProvider(
    override val providerName: String,
    override val location: String,
    override val providerURI: URI? = null,
    expiryDuration: Duration
) : JobsProvider {

    override val cachedJobs =
        SuspendingLazyWithExpiry(expiryDuration) { getJobs().flowOn(Dispatchers.Default).toList() }

    protected val logger = loggerFactory(this::class.java)

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