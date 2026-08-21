package com.fuelog.droid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fuelog.droid.R
import com.fuelog.droid.data.RefuelEntry
import com.fuelog.droid.ui.components.AutoSelectTextField
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRefuelScreen(
    viewModel: FuelViewModel,
    onBack: () -> Unit,
    onPickLocation: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val volumeUnit by viewModel.volumeUnit.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()
    val pickedLocation by viewModel.pickedLocation.collectAsState()
    val pickedAddress by viewModel.pickedAddress.collectAsState()
    val vehicleDetails by viewModel.vehicleDetails.collectAsState()
    val existingStations by viewModel.stations.collectAsState()

    var date by remember {
        val initialDate = selectedEntry?.date?.let { rawDate ->
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val dateObj = isoFormat.parse(rawDate)
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(dateObj!!)
            } catch (_: Exception) {
                rawDate
            }
        } ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        
        mutableStateOf(initialDate)
    }
    var fuelType by remember { mutableStateOf(selectedEntry?.fuelType ?: "LPG") }
    var tripDistanceType by remember { mutableStateOf(selectedEntry?.tripDistanceType ?: "Trip A") }
    var odometer by remember { mutableStateOf(selectedEntry?.odometer?.toString() ?: "") }
    var fuelConsumed by remember { mutableStateOf(selectedEntry?.fuelConsumed?.toString() ?: "") }
    var fuelPrice by remember { mutableStateOf(selectedEntry?.fuelPrice?.toString() ?: "") }
    var stationName by remember { mutableStateOf(selectedEntry?.stationName ?: "") }
    var paymentOption by remember { mutableStateOf(selectedEntry?.paymentOption ?: "VISA") }

    val fuelTypes = listOf("LPG", "Gasoline", "Super Gasoline", "Diesel")
    val tripTypes = listOf("Trip A", "Trip B")
    val paymentOptions = listOf("VISA", "KHQR ABA", "KHQR PPCB", "KHQR ACLEDA")

    val handleBack = {
        viewModel.selectEntry(null)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedEntry == null) stringResource(R.string.add_refuel_title) else stringResource(R.string.edit_refuel_title)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date
                AutoSelectTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(stringResource(R.string.date)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // Vehicle Info
                AutoSelectTextField(
                    value = vehicleDetails.plateNumber,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.vehicle_plate_settings)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false
                )

                // Fuel Type
                DropdownField(stringResource(R.string.fuel_type), fuelType, fuelTypes) { fuelType = it }

                // Trip Distance Type
                Text(stringResource(R.string.trip_type), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    tripTypes.forEach { type ->
                        RadioButton(
                            selected = tripDistanceType == type,
                            onClick = { tripDistanceType = type }
                        )
                        Text(type)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Numeric Inputs
                AutoSelectTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text(stringResource(R.string.odometer_reading, distanceUnit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                AutoSelectTextField(
                    value = fuelConsumed,
                    onValueChange = { fuelConsumed = it },
                    label = { Text(stringResource(R.string.fuel_consumed, volumeUnit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                AutoSelectTextField(
                    value = fuelPrice,
                    onValueChange = { fuelPrice = it },
                    label = { Text(stringResource(R.string.fuel_price, volumeUnit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Station Name with Autocomplete
                var stationExpanded by remember { mutableStateOf(false) }
                val filteredStations = remember(stationName, existingStations) {
                    if (stationName.isBlank()) emptyList()
                    else existingStations.filter { 
                        it.stationName.contains(stationName, ignoreCase = true) 
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = stationExpanded && filteredStations.isNotEmpty(),
                    onExpandedChange = { stationExpanded = it }
                ) {
                    AutoSelectTextField(
                        value = stationName,
                        onValueChange = { 
                            stationName = it
                            stationExpanded = true
                        },
                        label = { Text(stringResource(R.string.station_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = stationExpanded && filteredStations.isNotEmpty(),
                        onDismissRequest = { stationExpanded = false }
                    ) {
                        filteredStations.forEach { entry ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(entry.stationName)
                                        entry.location?.let { 
                                            Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) 
                                        }
                                    }
                                },
                                onClick = {
                                    stationName = entry.stationName
                                    stationExpanded = false
                                    // Auto-fill location
                                    if (entry.latitude != null && entry.longitude != null) {
                                        viewModel.pickLocation(entry.latitude, entry.longitude, entry.location)
                                    }
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().clickable { onPickLocation() }) {
                    AutoSelectTextField(
                        value = pickedAddress ?: "",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.location_tap)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false
                    )
                }

                // Payment Option
                DropdownField(stringResource(R.string.payment_option), paymentOption, paymentOptions) {
                    paymentOption = it
                }

                Button(
                    onClick = {
                        val currentOdo = odometer.toDoubleOrNull() ?: 0.0
                        val consumed = fuelConsumed.toDoubleOrNull() ?: 0.0
                        val price = fuelPrice.toDoubleOrNull() ?: 0.0
                        val calculatedTotal = consumed * price

                        val previousOdo = viewModel.entries.value
                            .filter { it.vehicleType == vehicleDetails.plateNumber && it.id != selectedEntry?.id }
                            .maxByOrNull { it.date }?.odometer ?: vehicleDetails.initialOdometer
                        
                        val calculatedDistance = if (currentOdo > previousOdo) currentOdo - previousOdo else 0.0

                        val entry = RefuelEntry(
                            id = selectedEntry?.id,
                            date = date,
                            vehicleType = vehicleDetails.plateNumber,
                            fuelType = fuelType,
                            tripDistanceType = tripDistanceType,
                            distance = if (selectedEntry != null && odometer == selectedEntry?.odometer.toString()) selectedEntry!!.distance else calculatedDistance,
                            odometer = currentOdo,
                            fuelConsumed = consumed,
                            fuelPrice = price,
                            stationName = stationName,
                            location = pickedAddress,
                            latitude = pickedLocation?.first,
                            longitude = pickedLocation?.second,
                            paymentOption = paymentOption,
                            status = selectedEntry?.status ?: 1,
                            totalPrice = calculatedTotal,
                            isSynced = false
                        )
                        viewModel.saveEntry(entry) {
                            handleBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && odometer.isNotBlank() && fuelConsumed.isNotBlank() && fuelPrice.isNotBlank() && stationName.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (selectedEntry == null) stringResource(R.string.save) else stringResource(R.string.update))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
