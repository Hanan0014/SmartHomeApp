package com.smarthome.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
){

    object Dashboard : BottomNavItem(
        "dashboard",
        "Dashboard",
        Icons.Default.Home
    )


    object FloorPlan : BottomNavItem(
        "floor_plan",
        "Floor Plan",
        Icons.Default.Map
    )


    object Reports : BottomNavItem(
        "reports",
        "Reports",
        Icons.Default.Assessment
    )
}