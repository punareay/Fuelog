package com.fuelog.droid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelog.droid.data.RefuelEntry
import com.fuelog.droid.data.RefuelRepository
import com.fuelog.droid.data.SettingsManager
import com.fuelog.droid.data.VehicleDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class ReportFilter {
    ALL, WEEKLY, MONTHLY, QUARTERLY, SEMESTER, YEARLY, CUSTOM
}

class FuelViewModel(
    private val repository: RefuelRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _selectedPlateNumber = MutableStateFlow(settingsManager.getSelectedPlateNumber())
    val selectedPlateNumber: StateFlow<String?> = _selectedPlateNumber.asStateFlow()

    val vehicles: StateFlow<List<VehicleDetails>> = repository.vehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allEntries = repository.entries.map { list ->
        list.sortedWith(compareByDescending<RefuelEntry> { it.date }.thenByDescending { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries = combine(allEntries, _selectedPlateNumber) { list, plate ->
        if (plate == null) list else list.filter { it.vehicleType == plate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicleDetails: StateFlow<VehicleDetails> = combine(vehicles, _selectedPlateNumber) { list, plate ->
        list.find { it.plateNumber == plate } ?: settingsManager.getVehicleDetails()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsManager.getVehicleDetails())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _distanceUnit = MutableStateFlow(settingsManager.getDistanceUnit())
    val distanceUnit: StateFlow<String> = _distanceUnit.asStateFlow()

    private val _volumeUnit = MutableStateFlow(settingsManager.getVolumeUnit())
    val volumeUnit: StateFlow<String> = _volumeUnit.asStateFlow()

    private val _selectedEntry = MutableStateFlow<RefuelEntry?>(null)
    val selectedEntry: StateFlow<RefuelEntry?> = _selectedEntry.asStateFlow()

    private val _pickedLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val pickedLocation: StateFlow<Pair<Double, Double>?> = _pickedLocation.asStateFlow()

    private val _pickedAddress = MutableStateFlow<String?>(null)
    val pickedAddress: StateFlow<String?> = _pickedAddress.asStateFlow()

    private val _reportFilter = MutableStateFlow(ReportFilter.ALL)
    val reportFilter: StateFlow<ReportFilter> = _reportFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    val filteredEntries: StateFlow<List<RefuelEntry>> = combine(
        entries,
        _reportFilter,
        _customDateRange
    ) { entriesList, filter, customRange ->
        filterEntries(entriesList, filter, customRange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
        refreshVehicles()
    }

    private fun filterEntries(
        entries: List<RefuelEntry>,
        filter: ReportFilter,
        customRange: Pair<Long, Long>?
    ): List<RefuelEntry> {
        if (filter == ReportFilter.ALL) return entries

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        val startTime: Long = when (filter) {
            ReportFilter.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.timeInMillis
            }
            ReportFilter.MONTHLY -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            ReportFilter.QUARTERLY -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.timeInMillis
            }
            ReportFilter.SEMESTER -> {
                calendar.add(Calendar.MONTH, -6)
                calendar.timeInMillis
            }
            ReportFilter.YEARLY -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.timeInMillis
            }
            ReportFilter.CUSTOM -> customRange?.first ?: 0L
            else -> 0L
        }

        val endTime: Long = if (filter == ReportFilter.CUSTOM) {
            customRange?.second ?: Long.MAX_VALUE
        } else {
            now
        }

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return entries.filter { entry ->
            try {
                val entryDate = isoFormat.parse(entry.date)?.time ?: 0L
                entryDate in startTime..endTime
            } catch (_: Exception) {
                val formats = listOf(
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                )
                var entryTime = 0L
                for (format in formats) {
                    try {
                        entryTime = format.parse(entry.date)?.time ?: 0L
                        if (entryTime != 0L) break
                    } catch (_: Exception) { }
                }
                entryTime in startTime..endTime
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshEntries()
            _isLoading.value = false
        }
    }

    fun refreshVehicles() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshVehicles()
            _isLoading.value = false
        }
    }

    fun setReportFilter(filter: ReportFilter) {
        _reportFilter.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = start to end
        _reportFilter.value = ReportFilter.CUSTOM
    }

    fun saveEntry(entry: RefuelEntry, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = if (entry.id == null) {
                repository.addEntry(entry)
            } else {
                repository.updateEntry(entry)
            }
            _isLoading.value = false
            if (success) {
                _toastMessage.emit(if (entry.id == null) "Added successfully" else "Updated successfully")
                onSuccess()
            } else {
                _toastMessage.emit("Failed to save. Please try again.")
            }
        }
    }

    fun deleteEntry(entry: RefuelEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteEntry(entry)
            _isLoading.value = false
            if (success) {
                _toastMessage.emit("Deleted successfully")
            } else {
                _toastMessage.emit("Failed to delete. Please try again.")
            }
        }
    }

    fun selectEntry(entry: RefuelEntry?) {
        _selectedEntry.value = entry
        if (entry != null) {
            _pickedLocation.value = if (entry.latitude != null && entry.longitude != null) {
                entry.latitude to entry.longitude
            } else null
            _pickedAddress.value = entry.location
        } else {
            _pickedLocation.value = null
            _pickedAddress.value = null
        }
    }

    fun pickLocation(lat: Double, lon: Double, address: String?) {
        _pickedLocation.value = lat to lon
        _pickedAddress.value = address
    }

    fun setDistanceUnit(unit: String) {
        settingsManager.setDistanceUnit(unit)
        _distanceUnit.value = unit
    }

    fun setVolumeUnit(unit: String) {
        settingsManager.setVolumeUnit(unit)
        _volumeUnit.value = unit
    }

    fun setVehicleDetails(details: VehicleDetails) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.addVehicle(details)
            _isLoading.value = false
            if (success) {
                settingsManager.saveVehicle(details)
                settingsManager.setSelectedPlateNumber(details.plateNumber)
                _selectedPlateNumber.value = details.plateNumber
            } else {
                _toastMessage.emit("Failed to save vehicle to cloud.")
            }
        }
    }

    fun selectVehicle(plateNumber: String) {
        settingsManager.setSelectedPlateNumber(plateNumber)
        // Also ensure the vehicle details are cached locally if they exist in the flow
        vehicles.value.find { it.plateNumber == plateNumber }?.let {
            settingsManager.saveVehicle(it)
        }
        _selectedPlateNumber.value = plateNumber
    }
}
