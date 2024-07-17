package com.jamhour.core.provider

import com.jamhour.core.job.Job
import java.net.URI

interface JobsProvider : Comparable<JobsProvider> {

    val providerName: String
    val providerDescription: String
    val providerURI: URI?
    val providerStatus: JobProviderStatus
    val providerJobs: List<Job>

}