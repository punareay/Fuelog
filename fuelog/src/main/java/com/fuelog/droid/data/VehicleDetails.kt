@file:OptIn(InternalSerializationApi::class)
package com.fuelog.droid.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDetails(
    val plateNumber: String = "",
    val model: String = "",
    val year: String = ""
)
