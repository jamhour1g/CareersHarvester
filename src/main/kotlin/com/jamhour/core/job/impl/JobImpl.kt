package com.jamhour.core.job.impl

import com.jamhour.core.job.Job
import com.jamhour.core.job.JobVacancyType
import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

@JvmRecord
data class JobImpl(
    override val jobTitle: String,
    override val jobLocation: String,
    override val jobDescription: String,
    override val jobResponsibilities: String,
    override val jobRequirements: String,
    override val jobPositionLevel: String,
    override val jobSalary: String,
    override val jobExperience: String,
    override val jobDegree: String,
    override val jobQualifications: String,
    override val jobInstructionsOnHowToApply: String,
    override val jobPreksAndBenefits: String,
    override val jobPublishDate: LocalDate?,
    override val jobDeadline: LocalDate?,
    override val typeOfVacancy: JobVacancyType,
    override val jobPoster: JobPoster,
    override val jobURI: URI,
) : Job
