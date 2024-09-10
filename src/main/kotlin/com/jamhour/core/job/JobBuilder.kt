package com.jamhour.core.job

import com.jamhour.core.job.impl.JobBuilderImpl
import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

interface JobBuilder {

    val jobPoster: JobPoster
    val jobURI: URI
    val jobTitle: String
    val jobLocation: String

    var jobDescription: String
    var jobResponsibilities: String
    var jobRequirements: String
    var jobPositionLevel: String
    var jobSalary: String
    var jobExperience: String
    var jobDegree: String
    var jobQualifications: String
    var jobInstructionsOnHowToApply: String
    var jobPreksAndBenefits: String
    var typeOfVacancy: JobVacancyType
    var jobPublishDate: LocalDate?
    var jobDeadline: LocalDate?

    fun build(): Job

}

fun buildJob(
    jobPoster: JobPoster,
    jobURI: URI,
    jobTitle: String,
    jobLocation: String,
    builderAction: (JobBuilder.() -> Unit)? = null
): Job {

    val jobBuilderImpl = JobBuilderImpl(
        jobPoster = jobPoster,
        jobURI = jobURI,
        jobTitle = jobTitle,
        jobLocation = jobLocation
    )

    builderAction?.let { jobBuilderImpl.apply { it() } }

    return jobBuilderImpl.build()
}