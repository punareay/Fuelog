package com.fuelog.droid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fuelog_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getDistanceUnit(): String {
        return prefs.getString("distance_unit", "km") ?: "km"
    }

    fun setDistanceUnit(unit: String) {
        prefs.edit { putString("distance_unit", unit) }
    }

    fun getVolumeUnit(): String {
        return prefs.getString("volume_unit", "L") ?: "L"
    }

    fun setVolumeUnit(unit: String) {
        prefs.edit { putString("volume_unit", unit) }
    }

    fun getVehicles(): List<VehicleDetails> {
        val jsonString = prefs.getString("vehicles_list", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<VehicleDetails>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveVehicle(vehicle: VehicleDetails) {
        val vehicles = getVehicles().toMutableList()
        val index = vehicles.indexOfFirst { it.plateNumber == vehicle.plateNumber }
        if (index != -1) {
            vehicles[index] = vehicle
        } else {
            vehicles.add(vehicle)
        }
        prefs.edit {
            putString("vehicles_list", json.encodeToString(vehicles))
        }
    }

    fun getSelectedPlateNumber(): String? {
        return prefs.getString("selected_plate_number", null)
    }

    fun setSelectedPlateNumber(plateNumber: String?) {
        prefs.edit { putString("selected_plate_number", plateNumber) }
    }

    fun getVehicleDetails(): VehicleDetails {
        val selectedPlate = getSelectedPlateNumber()
        val vehicles = getVehicles()
        return vehicles.find { it.plateNumber == selectedPlate } ?: VehicleDetails()
    }

    // --- Offline Entry Storage ---

    fun getCachedEntries(): List<RefuelEntry> {
        val jsonString = prefs.getString("cached_entries", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<RefuelEntry>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCachedEntries(entries: List<RefuelEntry>) {
        prefs.edit {
            putString("cached_entries", json.encodeToString(entries))
        }
    }

    fun updateSingleCachedEntry(entry: RefuelEntry) {
        val entries = getCachedEntries().toMutableList()
        // If entry has an ID, match by ID, otherwise match by timestamp (approximate)
        val index = if (entry.id != null) {
            entries.indexOfFirst { it.id == entry.id }
        } else {
            entries.indexOfFirst { it.date == entry.date && it.totalPrice == entry.totalPrice }
        }
        
        if (index != -1) {
            entries[index] = entry
        } else {
            entries.add(entry)
        }
        saveCachedEntries(entries)
    }
}
