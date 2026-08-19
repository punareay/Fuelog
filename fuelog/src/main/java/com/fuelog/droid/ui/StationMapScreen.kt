package com.fuelog.droid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuelog.droid.data.RefuelEntry
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationMapScreen(viewModel: FuelViewModel, onBack: () -> Unit) {
    val entries by viewModel.filteredEntries.collectAsState()
    
    // Group entries by location
    val stations = remember(entries) {
        entries.filter { it.latitude != null && it.longitude != null }
            .groupBy { "${it.latitude}_${it.longitude}" }
    }

    val cameraPositionState = rememberCameraPositionState {
        // Default to the most recent entry location or a default location
        val lastEntry = entries.firstOrNull { it.latitude != null && it.longitude != null }
        position = CameraPosition.fromLatLngZoom(
            if (lastEntry != null) LatLng(lastEntry.latitude!!, lastEntry.longitude!!) else LatLng(11.5564, 104.9282), // Phnom Penh default
            12f
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Station Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false), // App context might not have permission here
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            stations.forEach { (_, stationEntries) ->
                val first = stationEntries.first()
                val position = LatLng(first.latitude!!, first.longitude!!)
                
                Marker(
                    state = MarkerState(position = position),
                    title = first.stationName.ifBlank { "Unknown Station" },
                    snippet = "${stationEntries.size} entries here",
                    onClick = {
                        // Optional: Show details
                        false
                    }
                )
            }
        }
    }
}
