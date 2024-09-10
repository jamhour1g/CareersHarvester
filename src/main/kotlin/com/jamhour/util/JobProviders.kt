package com.jamhour.util

import com.jamhour.core.job.Job
import com.jamhour.core.provider.JobsProvider
import com.jamhour.core.provider.impl.json.AsalTech
import com.jamhour.core.provider.impl.json.Foothill
import com.jamhour.core.provider.impl.json.Harri
import com.jamhour.core.provider.impl.json.Userpilot
import com.jamhour.core.provider.impl.scraped.Foras
import com.jamhour.core.provider.impl.scraped.JobsDotPS
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture


val jobProviders: List<JobsProvider> = listOf(
    AsalTech(),
    Harri(),
    Userpilot(),
    Foothill(),
    JobsDotPS(),
    Foras(),
)

suspend fun getJobs() = coroutineScope {
    jobProviders.map { it.cachedJobs }
        .awaitAll()
        .flatten()
}

suspend fun getJobsAsDeferred() = coroutineScope {
    jobProviders.map { it.cachedJobs }
}

suspend fun getJobsFilterBy(filter: (Job) -> Boolean) = getJobs().filter(filter)

suspend fun getJobsFromProvider(provider: JobsProvider): List<Job> {
    validateProviderExists(provider)
    return provider.cachedJobs.await()
}

fun getJobsFromProviderAsDeferred(provider: JobsProvider): Deferred<List<Job>> {
    validateProviderExists(provider)
    return provider.cachedJobs
}

suspend fun getJobsFromProviderFilterBy(jobProvider: JobsProvider, filter: (Job) -> Boolean): List<Job> {
    validateProviderExists(jobProvider)
    return jobProviders.find { jobProvider == it }!!.cachedJobs.await().filter(filter)
}

private fun validateProviderExists(provider: JobsProvider) {
    require(provider in jobProviders) { "Job provider ${provider.providerName} is not registered in the list" }
}

fun getJobsProvidersFilterBy(filter: (JobsProvider) -> Boolean): List<JobsProvider> =
    jobProviders.filter(filter)

// Methods to interop with java

@DelicateCoroutinesApi
fun getJobsFromProviderAsCompletableFuture(provider: JobsProvider): CompletableFuture<List<Job>> =
    GlobalScope.future { getJobsFromProvider(provider) }


@DelicateCoroutinesApi
fun getJobsFromProviderFilterByAsCompletableFuture(
    jobProvider: JobsProvider,
    filter: (Job) -> Boolean
): CompletableFuture<List<Job>> =
    getJobsFromProviderAsCompletableFuture(jobProvider).thenApply { it.filter(filter) }

@DelicateCoroutinesApi
fun getJobsAsCompletableFuture() = GlobalScope.future {
    getJobs()
}

@DelicateCoroutinesApi
fun getJobsFilterByAsCompletableFuture(filter: (Job) -> Boolean): CompletableFuture<List<Job>> =
    getJobsAsCompletableFuture().thenApply { it.filter(filter) }
