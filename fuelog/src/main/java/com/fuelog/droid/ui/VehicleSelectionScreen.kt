package com.fuelog.droid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.fuelog.droid.data.VehicleDetails
import com.fuelog.droid.ui.components.AutoSelectTextField
import com.fuelog.droid.ui.viewmodel.FuelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionScreen(viewModel: FuelViewModel, onVehicleSelected: () -> Unit) {
    val vehicles by viewModel.vehicles.collectAsState()
    
    // Logic: If no vehicles exist, start with the Add form. 
    // If vehicles exist, show the list for selection.
    var showAddForm by remember(vehicles) { mutableStateOf(vehicles.isEmpty()) }
    
    var plateNumber by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vehicle Setup") })
        },
        floatingActionButton = {
            // Only show add button if we are currently looking at the list
            if (!showAddForm && vehicles.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showAddForm) {
                Text(
                    text = "Add New Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AutoSelectTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = { Text("Plate Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AutoSelectTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AutoSelectTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (plateNumber.isNotBlank()) {
                            viewModel.setVehicleDetails(
                                VehicleDetails(plateNumber, model, year)
                            )
                            onVehicleSelected()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plateNumber.isNotBlank()
                ) {
                    Text("Save & Start")
                }
                
                if (vehicles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddForm = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Back to List")
                    }
                }
            } else {
                Text(
                    text = "Select an existing vehicle:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(vehicles) { vehicle ->
                        VehicleItem(
                            vehicle = vehicle,
                            onClick = {
                                viewModel.selectVehicle(vehicle.plateNumber)
                                onVehicleSelected()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleItem(vehicle: VehicleDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = vehicle.plateNumber,
                    style = MaterialTheme.typography.titleLarge
                )
                if (vehicle.model.isNotBlank() || vehicle.year.isNotBlank()) {
                    Text(
                        text = "${vehicle.model} ${vehicle.year}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
