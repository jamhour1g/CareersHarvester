package com.jamhour.core.job.impl

import com.jamhour.core.job.Job
import com.jamhour.core.job.JobBuilder
import com.jamhour.core.job.JobVacancyType
import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

class JobBuilderImpl() : JobBuilder {

    private var jobTitle: String = ""
    private var jobLocation: String = ""
    private var jobDescription: String = ""
    private var jobResponsibilities: String = ""
    private var jobRequirements: String = ""
    private var jobPositionLevel: String = ""
    private var jobSalary: String = ""
    private var jobExperience: String = ""
    private var jobDegree: String = ""
    private var jobQualifications: String = ""
    private var jobInstructionsOnHowToApply: String = ""
    private var jobPreksAndBenefits: String = ""
    private var jobPublishDate: LocalDate? = null
    private var jobDeadline: LocalDate? = null
    private var typeOfVacancy: JobVacancyType = JobVacancyType.NOT_SPECIFIED
    private var jobPoster: JobPoster? = null
    private var jobURI: URI? = null

    override fun title(title: String) = apply { jobTitle = title }
    override fun location(location: String) = apply { jobLocation = location }
    override fun description(description: String) = apply { jobDescription = description }
    override fun responsibilities(responsibilities: String) = apply { jobResponsibilities = responsibilities }
    override fun qualifications(qualifications: String) = apply { jobQualifications = qualifications }
    override fun preksAndBenefits(preksAndBenefits: String) = apply { jobPreksAndBenefits = preksAndBenefits }
    override fun instructionsOnHowToApply(instructionsOnHowToApply: String) =
        apply { jobInstructionsOnHowToApply = instructionsOnHowToApply }

    override fun typeOfVacancy(typeOfVacancy: JobVacancyType) = apply { this.typeOfVacancy = typeOfVacancy }
    override fun poster(poster: JobPoster) = apply { jobPoster = poster }
    override fun requirements(requirements: String) = apply { jobRequirements = requirements }
    override fun positionLevel(positionLevel: String) = apply { jobPositionLevel = positionLevel }
    override fun salary(salary: String) = apply { jobSalary = salary }
    override fun experience(experience: String) = apply { jobExperience = experience }
    override fun degree(degree: String) = apply { jobDegree = degree }
    override fun jobPublishDate(publishDate: LocalDate) = apply { jobPublishDate = publishDate }
    override fun jobDeadline(deadline: LocalDate) = apply { jobDeadline = deadline }
    override fun jobURI(uri: URI) = apply { jobURI = uri }

    override fun build(): Job = JobImpl(
        jobTitle,
        jobLocation,
        jobDescription,
        jobResponsibilities,
        jobRequirements,
        jobPositionLevel,
        jobSalary,
        jobExperience,
        jobDegree,
        jobQualifications,
        jobInstructionsOnHowToApply,
        jobPreksAndBenefits,
        jobPublishDate,
        jobDeadline,
        typeOfVacancy,
        checkNotNull(jobPoster) { "JobPoster cannot be null" },
        checkNotNull(jobURI) { "JobURI cannot be null" }
    )


}