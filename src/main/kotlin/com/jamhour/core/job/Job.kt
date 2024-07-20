package com.jamhour.core.job

import com.jamhour.core.poster.JobPoster
import com.jamhour.core.poster.JobPosterContactInfo
import com.jamhour.core.provider.JobsProvider
import java.net.URI
import java.time.LocalDate

val jobComparator = Comparator.comparing<Job, String> {
    it.jobsProvider
        .providerURI
        ?.toString() ?: ""
}.thenComparing(Job::jobTitle)
    .thenComparing(Job::jobLocation)
    .thenComparing(Job::jobDescription)

interface Job : Comparable<Job> {

    val jobTitle: String
    val jobLocation: String
    val jobDescription: String
    val jobResponsibilities: String
    val jobRequirements: String
    val jobPositionLevel: String
    val jobSalary: String
    val jobExperience: String
    val jobDegree: String
    val jobQualifications: String
    val jobInstructionsOnHowToApply: String
    val jobPreksAndBenefits: String
    val jobPublishDate: LocalDate?
    val jobDeadline: LocalDate?
    val typeOfVacancy: JobVacancyType
    val jobPoster: JobPoster
    val jobPosterContactInfo: JobPosterContactInfo
    val jobsProvider: JobsProvider
    val jobURI: URI
    val jobBuilder: JobBuilder

    override fun compareTo(other: Job): Int {
        return jobComparator.compare(this, other)
    }
}