package com.jamhour.core.poster

import java.net.URI

interface JobPosterContactInfo {
    val email: String
    val phoneNumber: String
    val address: String
    val website: String
    val websiteURI: URI?
}