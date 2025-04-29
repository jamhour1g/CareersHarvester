package com.jamhour.core.job.impl

import com.jamhour.core.job.Job
import com.jamhour.core.job.JobBuilder
import com.jamhour.core.job.JobVacancyType
import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

class JobBuilderImpl(
    override val jobPoster: JobPoster,
    override val jobURI: URI,
    override val jobTitle: String,
    override val jobLocation: String
) : JobBuilder {

    override var jobDescription: String = ""
    override var jobResponsibilities: String = ""
    override var jobRequirements: String = ""
    override var jobPositionLevel: String = ""
    override var jobSalary: String = ""
    override var jobExperience: String = ""
    override var jobDegree: String = ""
    override var jobQualifications: String = ""
    override var jobInstructionsOnHowToApply: String = ""
    override var jobPreksAndBenefits: String = ""
    override var typeOfVacancy: JobVacancyType = JobVacancyType.NOT_SPECIFIED
    override var jobPublishDate: LocalDate? = null
    override var jobDeadline: LocalDate? = null


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
        jobPoster,
        jobURI
    )


}