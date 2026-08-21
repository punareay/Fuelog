package com.fuelog.droid.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.fuelog.droid.R
import com.fuelog.droid.ui.components.formatRiel
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationMapScreen(viewModel: FuelViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by viewModel.filteredEntries.collectAsState()
    
    // Group entries by location
    val stations = remember(entries) {
        entries.filter { it.latitude != null && it.longitude != null }
            .groupBy { "${it.latitude}_${it.longitude}" }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(11.5564, 104.9282), 12f)
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
    }

    // Function to zoom to current location
    fun zoomToCurrentLocation() {
        if (locationPermissionGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    15f
                                )
                            )
                        }
                    }
                }
            } catch (_: SecurityException) { }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Function to zoom to all pins
    fun zoomToAllPins() {
        if (stations.isEmpty()) return
        
        val builder = LatLngBounds.Builder()
        stations.forEach { (_, stationEntries) ->
            val first = stationEntries.first()
            builder.include(LatLng(first.latitude!!, first.longitude!!))
        }
        val bounds = builder.build()
        
        scope.launch {
            if (stations.size == 1) {
                // If only one station, just zoom to it specifically
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(bounds.center, 15f)
                )
            } else {
                // Zoom to fit all stations with padding
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 150)
                )
            }
        }
    }

    // On Start: zoom to current location
    LaunchedEffect(Unit) {
        if (locationPermissionGranted) {
            zoomToCurrentLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.station_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { zoomToCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.my_location))
                    }
                    IconButton(onClick = { zoomToAllPins() }) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = stringResource(R.string.view_all_pins))
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
            properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            stations.forEach { (_, stationEntries) ->
                val first = stationEntries.first()
                val position = LatLng(first.latitude!!, first.longitude!!)
                
                Marker(
                    state = MarkerState(position = position),
                    title = first.stationName.ifBlank { "Unknown Station" },
                    snippet = stringResource(
                        R.string.refuels_count,
                        stationEntries.size,
                        formatRiel(stationEntries.sumOf { it.totalCost })
                    )
                )
            }
        }
    }
}
