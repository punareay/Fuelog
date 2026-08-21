@file:Suppress("DEPRECATION")

package com.fuelog.droid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fuelog.droid.R
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(viewModel: FuelViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickedLocation by viewModel.pickedLocation.collectAsState()
    
    var markerPosition by remember {
        mutableStateOf(pickedLocation?.let { LatLng(it.first, it.second) } ?: LatLng(11.5564, 104.9282)) // Default to Phnom Penh
    }
    
    var address by remember { mutableStateOf("") }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 15f)
    }

    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    fun updateAddress(latLng: LatLng) {
        scope.launch(IO) {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        address = addresses[0].getAddressLine(0) ?: "Unknown Address"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    address = "Error fetching address"
                }
            }
        }
    }

    LaunchedEffect(markerPosition) {
        updateAddress(markerPosition)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permission granted
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] != true && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] != true -> {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                markerPosition = currentLatLng
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_tap)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.pickLocation(markerPosition.latitude, markerPosition.longitude, address)
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    markerPosition = latLng
                },
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                Marker(
                    state = MarkerState(position = markerPosition),
                    title = stringResource(R.string.location_tap),
                    draggable = true
                )
            }
            
            // Custom UI Controls (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // My Location Button
                SmallFloatingActionButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        getCurrentLocation()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.my_location))
                }

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

            if (address.isNotEmpty()) {
                CustomCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = address,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CustomCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        content()
    }
}
