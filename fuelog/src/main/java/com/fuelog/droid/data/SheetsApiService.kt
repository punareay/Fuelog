@file:OptIn(InternalSerializationApi::class)
package com.fuelog.droid.data

import kotlinx.serialization.InternalSerializationApi
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VehicleRequest(val action: String, val data: VehicleDetails)

@Serializable
data class RefuelRequest(val action: String, val data: RefuelEntry)

class SheetsApiService {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
        followRedirects = true
    }

    // Replace with your Google Apps Script Web App URL
    private val baseUrl = "https://script.google.com/macros/s/AKfycby646bGMLtEn3BB1llyNv6pDDO48L8DKLlseiT7bmNigAuoxayD_usv1Xd4AhcfxtIo/exec"

    suspend fun getRefuelEntries(): List<RefuelEntry> {
        return try {
            Log.d("SheetsApiService", "Fetching entries from: $baseUrl")
            val response: HttpResponse = client.get("$baseUrl?type=refuel")
            val bodyText = response.bodyAsText()
            Log.d("SheetsApiService", "Refuel Response: ${response.status}")
            
            if (response.status == HttpStatusCode.OK) {
                if (bodyText.isBlank()) return emptyList()
                try {
                    val sheetResponse = jsonConfig.decodeFromString<SheetResponse>(bodyText)
                    sheetResponse.data.filter { it.status != 0 }
                } catch (e: Exception) {
                    Log.e("SheetsApiService", "Refuel JSON Decoding failed", e)
                    Log.e("SheetsApiService", "Raw Body: $bodyText")
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SheetsApiService", "Network error refuel", e)
            emptyList()
        }
    }

    suspend fun getVehicles(): List<VehicleDetails> {
        return try {
            Log.d("SheetsApiService", "Fetching vehicles from: $baseUrl")
            val response: HttpResponse = client.get("$baseUrl?type=vehicles")
            val bodyText = response.bodyAsText()
            Log.d("SheetsApiService", "Vehicle List Response: ${response.status}")
            
            if (response.status == HttpStatusCode.OK) {
                if (bodyText.isBlank()) return emptyList()
                try {
                    val vehicleResponse = jsonConfig.decodeFromString<VehicleResponse>(bodyText)
                    vehicleResponse.data
                } catch (e: Exception) {
                    Log.e("SheetsApiService", "Vehicle JSON Decoding failed", e)
                    Log.e("SheetsApiService", "Raw Body: $bodyText")
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SheetsApiService", "Error fetching vehicles", e)
            emptyList()
        }
    }

    suspend fun saveRefuelEntry(entry: RefuelEntry): Boolean {
        return callRefuelApi("add_refuel", entry)
    }

    suspend fun updateRefuelEntry(entry: RefuelEntry): Boolean {
        return callRefuelApi("update_refuel", entry)
    }

    suspend fun deleteRefuelEntry(entry: RefuelEntry): Boolean {
        return callRefuelApi("update_refuel", entry.copy(status = 0))
    }

    suspend fun addVehicle(vehicle: VehicleDetails): Boolean {
        return try {
            Log.d("SheetsApiService", "Adding vehicle: ${vehicle.plateNumber}")
            val response: HttpResponse = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(VehicleRequest("add_vehicle", vehicle))
            }
            val bodyText = response.bodyAsText()
            Log.d("SheetsApiService", "Add Vehicle Status: ${response.status}")
            Log.d("SheetsApiService", "Add Vehicle Body: $bodyText")
            // Even if it's 200 OK, check if GAS returned a "success" indicator or handled it
            // Redirection (302) is handled by Ktor, but GAS results often come in body
            isSuccess(response.status, bodyText)
        } catch (e: Exception) {
            Log.e("SheetsApiService", "Error adding vehicle", e)
            false
        }
    }

    private suspend fun callRefuelApi(action: String, entry: RefuelEntry): Boolean {
        return try {
            Log.d("SheetsApiService", "Calling refuel action: $action")
            val response: HttpResponse = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(RefuelRequest(action, entry))
            }
            val bodyText = response.bodyAsText()
            Log.d("SheetsApiService", "Refuel API Status: ${response.status}")
            Log.d("SheetsApiService", "Refuel API Body: $bodyText")
            isSuccess(response.status, bodyText)
        } catch (e: Exception) {
            Log.e("SheetsApiService", "Refuel API Error", e)
            false
        }
    }

    private fun isSuccess(status: HttpStatusCode, body: String): Boolean {
        // Broad success criteria for Google Apps Script
        return status == HttpStatusCode.OK || 
               status == HttpStatusCode.Found || 
               status == HttpStatusCode.MovedPermanently ||
               body.contains("success", ignoreCase = true) ||
               body.isBlank() // Sometimes GAS returns empty but processed successfully
    }
}
