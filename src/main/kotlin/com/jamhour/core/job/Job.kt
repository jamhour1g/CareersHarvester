package com.jamhour.core.job

import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

val jobComparator: Comparator<Job> =
    Comparator.comparing<Job, String> { it.getJobProvider().providerURI?.toString() ?: "" }
        .thenComparing(Job::jobTitle)
        .thenComparing(Job::jobLocation)
        .thenComparing(Job::jobDescription)

interface Job : Comparable<Job> {

    val jobTitle: String
    val jobLocation: String
    val jobURI: URI
    val jobPoster: JobPoster

    val jobDescription: String get() = ""
    val jobResponsibilities: String get() = ""
    val jobRequirements: String get() = ""
    val jobPositionLevel: String get() = ""
    val jobSalary: String get() = ""
    val jobExperience: String get() = ""
    val jobDegree: String get() = ""
    val jobQualifications: String get() = ""
    val jobInstructionsOnHowToApply: String get() = ""
    val jobPreksAndBenefits: String get() = ""
    val typeOfVacancy: JobVacancyType get() = JobVacancyType.NOT_SPECIFIED
    val jobPublishDate: LocalDate? get() = null
    val jobDeadline: LocalDate? get() = null

    override fun compareTo(other: Job) = jobComparator.compare(this, other)
    fun getJobProvider() = jobPoster.posterProvider

}