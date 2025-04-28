package com.jamhour.core.provider.impl.json

import com.jamhour.util.toURI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class Userpilot(expiryDuration: Duration = 1.days) : AbstractBambooHRJobProvider(
    NAME,
    LOCATION,
    providerURI,
    careerPageLink,
    expiryDuration
) {

    override fun filterJob(job: BambooHRJob): Boolean {
        val isInPalestine = job.location.toJobLocation() == LOCATION
        val isInSoftware = job.departmentId == SOFTWARE_DEPARTMENT_CODE
        return (isInSoftware && isInPalestine) || (isInSoftware && job.isRemote == true)
    }

    companion object {
        private const val LOCATION = "West Bank,Ramallah"
        private const val NAME = "Userpilot"
        private const val SOFTWARE_DEPARTMENT_CODE = 18573
        private val providerURI = "https://www.userpilot.com/".toURI()
        private val careerPageLink = "https://userpilot.bamboohr.com/careers".toURI()
    }
}

class Foothill(expiryDuration: Duration = 1.days) : AbstractBambooHRJobProvider(
    NAME,
    LOCATION,
    providerURI,
    careerPageLink,
    expiryDuration
) {

    override fun filterJob(job: BambooHRJob): Boolean = job.departmentId == SOFTWARE_DEPARTMENT_CODE

    companion object {
        private const val NAME = "FoothillSolutions"
        private const val LOCATION = "Nablus"
        private const val SOFTWARE_DEPARTMENT_CODE = 18504
        private val providerURI = "https://www.foothillsolutions.com/".toURI()
        private val careerPageLink = "https://foothillsolutions.bamboohr.com/careers".toURI()
    }

}

