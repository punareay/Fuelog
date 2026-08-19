package com.fuelog.droid.data

import kotlinx.serialization.Serializable

@Serializable
data class VehicleDetails(
    val plateNumber: String = "",
    val model: String = "",
    val year: String = ""
)
