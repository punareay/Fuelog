package com.fuelog.droid.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddRefuel : Screen("add_refuel")
    object History : Screen("history")
    object Settings : Screen("settings")
    object LocationPicker : Screen("location_picker")
    object VehicleSelection : Screen("vehicle_selection")
}
