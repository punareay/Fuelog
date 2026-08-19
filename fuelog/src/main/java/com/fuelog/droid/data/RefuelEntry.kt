@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
package com.fuelog.droid.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SheetResponse(
    val status: String,
    val data: List<RefuelEntry> = emptyList(),
    val message: String? = null
)

@Serializable
data class VehicleResponse(
    val status: String,
    val data: List<VehicleDetails> = emptyList()
)

@Serializable
data class RefuelEntry(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int? = null,
    val date: String,
    val vehicleType: String = "",
    val fuelType: String = "",
    val tripDistanceType: String = "Trip A",
    @SerialName("tripDistance") 
    @Serializable(with = DoubleOrEmptyStringSerializer::class)
    val distance: Double = 0.0,
    @Serializable(with = DoubleOrEmptyStringSerializer::class)
    val fuelConsumed: Double = 0.0,
    @Serializable(with = DoubleOrEmptyStringSerializer::class) 
    val fuelPrice: Double = 0.0,
    val stationName: String = "",
    val location: String? = null,
    @Serializable(with = NullableDoubleOrEmptyStringSerializer::class)
    val latitude: Double? = null,
    @Serializable(with = NullableDoubleOrEmptyStringSerializer::class)
    val longitude: Double? = null,
    val paymentOption: String = "",
    @Serializable(with = FlexibleIntSerializer::class) 
    val status: Int? = 1, // 1 = enable, 0 = disable
    @SerialName("totalPrice") 
    @Serializable(with = DoubleOrEmptyStringSerializer::class)
    val totalPrice: Double = 0.0,
    @Transient val isSynced: Boolean = true // Local-only field
) {
    val totalCost: Double get() = if (totalPrice > 0) totalPrice else fuelConsumed * fuelPrice
}

object DoubleOrEmptyStringSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DoubleOrEmpty", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val input = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            val content = element.content
            if (content.isBlank()) return 0.0
            val sanitized = content.replace(",", "")
            return sanitized.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }
}

object NullableDoubleOrEmptyStringSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableDoubleOrEmpty", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val input = decoder as? JsonDecoder ?: return try { decoder.decodeDouble() } catch(_: Exception) { null }
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            val content = element.content
            if (content.isBlank()) return null
            val sanitized = content.replace(",", "")
            return sanitized.toDoubleOrNull()
        }
        return null
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val input = decoder as? JsonDecoder ?: return try { decoder.decodeInt() } catch(_: Exception) { null }
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            val content = element.content
            if (content.isBlank()) return null
            val sanitized = content.replace(",", "")
            return sanitized.toDoubleOrNull()?.toInt()
        }
        return null
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeInt(value)
        }
    }
}
