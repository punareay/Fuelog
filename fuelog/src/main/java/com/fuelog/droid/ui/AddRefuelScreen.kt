package com.fuelog.droid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    var date by remember {
        val initialDate = selectedEntry?.date?.let { rawDate ->
            try {
                // Try parsing ISO format: 2026-03-10T06:49:00.000Z
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = isoFormat.parse(rawDate)
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date!!)
            } catch (_: Exception) {
                rawDate // Fallback if parsing fails
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
                title = { Text(if (selectedEntry == null) "Add Refuel Record" else "Edit Refuel Record") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // Vehicle Info (ReadOnly display of plate from settings)
                AutoSelectTextField(
                    value = vehicleDetails.plateNumber,
                    onValueChange = { },
                    label = { Text("Vehicle Plate (from Settings)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false
                )

                // Fuel Type
                DropdownField("Fuel Type", fuelType, fuelTypes) { fuelType = it }

                // Trip Distance Type
                Text("Trip Type", style = MaterialTheme.typography.labelLarge)
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
                    label = { Text("Odometer Reading ($distanceUnit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                AutoSelectTextField(
                    value = fuelConsumed,
                    onValueChange = { fuelConsumed = it },
                    label = { Text("Fuel Consumed ($volumeUnit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                AutoSelectTextField(
                    value = fuelPrice,
                    onValueChange = { fuelPrice = it },
                    label = { Text("Fuel Price (៛/$volumeUnit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Text Inputs
                AutoSelectTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("Station Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth().clickable { onPickLocation() }) {
                    AutoSelectTextField(
                        value = pickedAddress ?: "",
                        onValueChange = { },
                        label = { Text("Location (Tap to select)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false
                    )
                }

                // Payment Option
                DropdownField("Payment Option", paymentOption, paymentOptions) {
                    paymentOption = it
                }

                Button(
                    onClick = {
                        val currentOdo = odometer.toDoubleOrNull() ?: 0.0
                        val consumed = fuelConsumed.toDoubleOrNull() ?: 0.0
                        val price = fuelPrice.toDoubleOrNull() ?: 0.0
                        val calculatedTotal = consumed * price

                        // Calculate distance from previous ODO
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
                        Text(if (selectedEntry == null) "Save" else "Update")
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
