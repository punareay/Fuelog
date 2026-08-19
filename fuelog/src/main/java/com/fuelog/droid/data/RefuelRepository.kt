package com.fuelog.droid.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RefuelRepository(
    private val apiService: SheetsApiService,
    private val settingsManager: SettingsManager
) {

    private val _entries = MutableStateFlow<List<RefuelEntry>>(settingsManager.getCachedEntries())
    val entries: Flow<List<RefuelEntry>> = _entries.asStateFlow()

    private val _vehicles = MutableStateFlow<List<VehicleDetails>>(settingsManager.getVehicles())
    val vehicles: Flow<List<VehicleDetails>> = _vehicles.asStateFlow()

    suspend fun refreshEntries() {
        // Try to sync pending local entries first
        syncPendingEntries()

        val fetchedEntries = apiService.getRefuelEntries()
        if (fetchedEntries.isNotEmpty()) {
            Log.d("RefuelRepository", "Updating repository with ${fetchedEntries.size} entries")
            // Merge cloud entries with local unsynced entries
            val unsynced = settingsManager.getCachedEntries().filter { !it.isSynced }
            val combined = fetchedEntries.map { it.copy(isSynced = true) } + unsynced
            
            _entries.value = combined
            settingsManager.saveCachedEntries(combined)
        }
    }

    private suspend fun syncPendingEntries() {
        val pending = settingsManager.getCachedEntries().filter { !it.isSynced }
        if (pending.isEmpty()) return

        Log.d("RefuelRepository", "Attempting to sync ${pending.size} pending entries")
        pending.forEach { entry ->
            val success = if (entry.id == null) {
                apiService.saveRefuelEntry(entry)
            } else {
                apiService.updateRefuelEntry(entry)
            }
            if (success) {
                // We don't update individual items here to avoid many DB writes, 
                // just rely on the next refresh to get clean data.
                // But for robust offline, we should mark as synced.
            }
        }
    }

    suspend fun refreshVehicles() {
        val fetchedVehicles = apiService.getVehicles()
        if (fetchedVehicles.isNotEmpty()) {
            _vehicles.value = fetchedVehicles
            // Update local vehicle list cache too
            fetchedVehicles.forEach { settingsManager.saveVehicle(it) }
        }
    }

    suspend fun addEntry(entry: RefuelEntry): Boolean {
        // 1. Save locally first (marked as unsynced)
        val localEntry = entry.copy(isSynced = false)
        settingsManager.updateSingleCachedEntry(localEntry)
        _entries.value = settingsManager.getCachedEntries()

        // 2. Try to sync to cloud
        val success = apiService.saveRefuelEntry(entry)
        if (success) {
            // 3. If success, mark as synced and refresh
            settingsManager.updateSingleCachedEntry(entry.copy(isSynced = true))
            refreshEntries()
        }
        
        // Return true because it's "Saved" (locally at least)
        return true 
    }

    suspend fun updateEntry(entry: RefuelEntry): Boolean {
        val localEntry = entry.copy(isSynced = false)
        settingsManager.updateSingleCachedEntry(localEntry)
        _entries.value = settingsManager.getCachedEntries()

        val success = apiService.updateRefuelEntry(entry)
        if (success) {
            settingsManager.updateSingleCachedEntry(entry.copy(isSynced = true))
            refreshEntries()
        }
        return true
    }

    suspend fun deleteEntry(entry: RefuelEntry): Boolean {
        // For delete, we try cloud first because soft-delete is just an update
        val success = apiService.deleteRefuelEntry(entry)
        if (success) {
            refreshEntries()
        } else {
            // If offline, mark locally as disabled (status 0) and unsynced
            val localEntry = entry.copy(status = 0, isSynced = false)
            settingsManager.updateSingleCachedEntry(localEntry)
            _entries.value = settingsManager.getCachedEntries().filter { it.status != 0 }
        }
        return true
    }

    suspend fun addVehicle(vehicle: VehicleDetails): Boolean {
        val success = apiService.addVehicle(vehicle)
        if (success) {
            refreshVehicles()
        } else {
            // Offline vehicle add: save locally
            settingsManager.saveVehicle(vehicle)
            _vehicles.value = settingsManager.getVehicles()
        }
        return true // Success for local save
    }

    fun getTotalExpenses(): Double {
        return _entries.value.sumOf { it.totalCost }
    }
}
