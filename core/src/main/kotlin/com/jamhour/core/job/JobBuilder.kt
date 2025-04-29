package com.jamhour.core.job

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