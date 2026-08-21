package com.fuelog.droid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.fuelog.droid.R
import com.fuelog.droid.data.VehicleDetails
import com.fuelog.droid.ui.components.AutoSelectTextField
import com.fuelog.droid.ui.viewmodel.FuelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FuelViewModel,
    onBack: () -> Unit,
    onSwitchVehicle: () -> Unit
) {
    val currentDistanceUnit by viewModel.distanceUnit.collectAsState()
    val currentVolumeUnit by viewModel.volumeUnit.collectAsState()
    val currentVehicleDetails by viewModel.vehicleDetails.collectAsState()

    var plateNumber by remember { mutableStateOf(currentVehicleDetails.plateNumber) }
    var model by remember { mutableStateOf(currentVehicleDetails.model) }
    var year by remember { mutableStateOf(currentVehicleDetails.year) }
    var distanceUnit by remember { mutableStateOf(currentDistanceUnit) }
    var volumeUnit by remember { mutableStateOf(currentVolumeUnit) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.vehicle_identity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AutoSelectTextField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = { Text(stringResource(R.string.plate_number)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoSelectTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.model)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoSelectTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text(stringResource(R.string.year)) },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            Text(
                text = stringResource(R.string.distance_unit),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = distanceUnit == "km",
                    onClick = { distanceUnit = "km" }
                )
                Text(stringResource(R.string.km))
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                RadioButton(
                    selected = distanceUnit == "miles",
                    onClick = { distanceUnit = "miles" }
                )
                Text(stringResource(R.string.miles))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.volume_unit),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = volumeUnit == "L",
                    onClick = { volumeUnit = "L" }
                )
                Text(stringResource(R.string.liters))
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                RadioButton(
                    selected = volumeUnit == "gal",
                    onClick = { volumeUnit = "gal" }
                )
                Text(stringResource(R.string.gallons))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSwitchVehicle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.switch_add_vehicle))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.setVehicleDetails(
                        VehicleDetails(
                            plateNumber = plateNumber,
                            model = model,
                            year = year
                        )
                    )
                    viewModel.setDistanceUnit(distanceUnit)
                    viewModel.setVolumeUnit(volumeUnit)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
