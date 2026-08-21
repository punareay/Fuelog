package com.fuelog.droid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.fuelog.droid.data.RefuelRepository
import com.fuelog.droid.data.SettingsManager
import com.fuelog.droid.data.SheetsApiService
import com.fuelog.droid.ui.AddRefuelScreen
import com.fuelog.droid.ui.DashboardScreen
import com.fuelog.droid.ui.HistoryScreen
import com.fuelog.droid.ui.LocationPickerScreen
import com.fuelog.droid.ui.Screen
import com.fuelog.droid.ui.SettingsScreen
import com.fuelog.droid.ui.StationMapScreen
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import com.fuelog.droid.ui.VehicleSelectionScreen
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple manual dependency injection
        val settingsManager = SettingsManager(this)
        val apiService = SheetsApiService()
        val repository = RefuelRepository(apiService, settingsManager)
        val viewModel = FuelViewModel(repository, settingsManager)

        setContent {
            MaterialTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: FuelViewModel) {
    val context = LocalContext.current
    val selectedPlate by viewModel.selectedPlateNumber.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val startDestination = if (selectedPlate == null) Screen.VehicleSelection.route else Screen.Dashboard.route

    val items = listOf(
        Screen.Dashboard,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != Screen.AddRefuel.route && 
                currentDestination?.route != Screen.VehicleSelection.route &&
                currentDestination?.route != Screen.LocationPicker.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Dashboard -> Icons.Default.Home
                                        Screen.History -> Icons.Default.DateRange
                                        Screen.Settings -> Icons.Default.Settings
                                        else -> Icons.Default.Home
                                    },
                                    contentDescription = null
                                )
                            },
                            label = {
                                val labelRes = when (screen) {
                                    Screen.Dashboard -> R.string.nav_dashboard
                                    Screen.History -> R.string.nav_history
                                    Screen.Settings -> R.string.nav_settings
                                    else -> 0
                                }
                                if (labelRes != 0) {
                                    Text(stringResource(labelRes))
                                } else {
                                    Text(screen.route.replaceFirstChar { it.uppercase() })
                                }
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel) {
                    navController.navigate(Screen.AddRefuel.route)
                }
            }
            composable(Screen.History.route) {
                HistoryScreen(viewModel, {
                    navController.navigate(Screen.HistoryMap.route)
                }) {
                    navController.navigate(Screen.AddRefuel.route)
                }
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel, {
                    navController.popBackStack()
                }, {
                    navController.navigate(Screen.VehicleSelection.route)
                })
            }
            composable(Screen.VehicleSelection.route) {
                VehicleSelectionScreen(viewModel) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.VehicleSelection.route) { inclusive = true }
                    }
                }
            }
            composable(Screen.AddRefuel.route) {
                AddRefuelScreen(viewModel, {
                    navController.popBackStack()
                }, {
                    navController.navigate(Screen.LocationPicker.route)
                })
            }
            composable(Screen.LocationPicker.route) {
                LocationPickerScreen(viewModel) {
                    navController.popBackStack()
                }
            }
            composable(Screen.HistoryMap.route) {
                StationMapScreen(viewModel) {
                    navController.popBackStack()
                }
            }
        }
    }
}
