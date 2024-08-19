package com.jamhour.core.job

import com.jamhour.core.poster.JobPoster
import java.net.URI
import java.time.LocalDate

interface JobBuilder {

    fun title(title: String): JobBuilder
    fun location(location: String): JobBuilder
    fun description(description: String): JobBuilder
    fun responsibilities(responsibilities: String): JobBuilder
    fun qualifications(qualifications: String): JobBuilder
    fun preksAndBenefits(preksAndBenefits: String): JobBuilder
    fun instructionsOnHowToApply(instructionsOnHowToApply: String): JobBuilder
    fun typeOfVacancy(typeOfVacancy: JobVacancyType): JobBuilder
    fun poster(poster: JobPoster): JobBuilder
    fun requirements(requirements: String): JobBuilder
    fun positionLevel(positionLevel: String): JobBuilder
    fun salary(salary: String): JobBuilder
    fun experience(experience: String): JobBuilder
    fun degree(degree: String): JobBuilder
    fun jobPublishDate(jobPublishDate: LocalDate): JobBuilder
    fun jobDeadline(jobDeadline: LocalDate): JobBuilder
    fun jobURI(jobURI: URI): JobBuilder
    fun build(): Job

}