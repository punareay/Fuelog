package com.fuelog.droid.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(bounds.center, 15f)
                )
            } else {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 150)
                )
            }
        }
    }

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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
                uiSettings = MapUiSettings(zoomControlsEnabled = false) // Custom zoom controls
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

            // Custom UI Controls (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // My Location
                SmallFloatingActionButton(
                    onClick = { zoomToCurrentLocation() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.my_location))
                }

                // View All Pins
                SmallFloatingActionButton(
                    onClick = { zoomToAllPins() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = stringResource(R.string.view_all_pins))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom In
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }
        }
    }
}
