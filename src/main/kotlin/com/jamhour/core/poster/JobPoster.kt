package com.jamhour.core.poster

import com.jamhour.core.job.Job
import com.jamhour.core.provider.JobProviderVerification
import com.jamhour.core.provider.JobsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

val jobPosterComparator: Comparator<JobPoster> =
    Comparator.comparing<JobPoster, String> { it.posterUriOnProvider?.toString() ?: "" }
        .thenComparing(JobPoster::posterName)
        .thenComparing(JobPoster::posterLocation)

interface JobPoster : Comparable<JobPoster>, CoroutineScope {

    val posterName: String
    val posterLocation: String
    val posterProvider: JobsProvider
    val cachedJobsFromPoster: Deferred<List<Job>>

    val businessType: JobPosterBusinessType get() = JobPosterBusinessType.OTHER
    val posterOverview: String get() = ""
    val providerVerification: JobProviderVerification get() = JobProviderVerification.NOT_SUPPORTED
    val posterUriOnProvider: URI? get() = null
    val posterWebsite: URI? get() = null
    val posterEstablishmentDate: LocalDate? get() = null
    val posterContactInfo: JobPosterContactInfo? get() = null

    override fun compareTo(other: JobPoster) = jobPosterComparator.compare(this, other)
    override val coroutineContext: CoroutineContext get() = posterProvider.coroutineContext

    suspend fun getAllJobsFromProvider() = posterProvider.cachedJobs.await()
        .asSequence()
        .filter { it.jobPoster == this }
        .toList()

}
