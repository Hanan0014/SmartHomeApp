package com.smarthome.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.Floor
import com.smarthome.app.ui.dashboard.DashboardScreen
import com.smarthome.app.ui.device.DeviceDetailScreen
import com.smarthome.app.ui.floorplan.FloorPlanScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val FLOOR_PLAN = "floor_plan"
    const val DEVICE_DETAIL = "device_detail"
}

/**
 * In-memory selection holders. For a mini-project this avoids the overhead
 * of Gson-serializing complex objects into nav route arguments.
 */
private var selectedFloor: Floor? = null
private var selectedDevice: Device? = null

@Composable
fun SmartHomeNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onFloorSelected = { floor ->
                selectedFloor = floor
                navController.navigate(Routes.FLOOR_PLAN)
            })
        }
        composable(Routes.FLOOR_PLAN) {
            val floor = selectedFloor ?: return@composable
            FloorPlanScreen(
                floor = floor,
                onBack = { navController.popBackStack() },
                onDeviceSelected = { device ->
                    selectedDevice = device
                    navController.navigate(Routes.DEVICE_DETAIL)
                }
            )
        }
        composable(Routes.DEVICE_DETAIL) {
            val floor = selectedFloor ?: return@composable
            val device = selectedDevice ?: return@composable
            DeviceDetailScreen(
                floorId = floor.id,
                device = device,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
