package com.smarthome.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

import com.smarthome.app.ui.dashboard.DashboardViewModel
import com.smarthome.app.ui.dashboard.DashboardScreen
import com.smarthome.app.ui.device.DeviceDetailScreen
import com.smarthome.app.ui.floorplan.FloorPlanScreen
import com.smarthome.app.ui.floorplan.FloorPlanViewModel
import com.smarthome.app.ui.reports.ReportsScreen
import com.smarthome.app.ui.auth.LoginScreen
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.Floor


private object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val FLOOR_PLAN = "floor_plan"
    const val DEVICE_DETAIL = "device_detail"
    const val REPORTS = "reports"
}


private var selectedFloor: Floor? = null
private var selectedDevice: Device? = null

@Composable
fun SmartHomeNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {

    NavHost(navController = navController, startDestination = Routes.LOGIN, modifier = modifier) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onFloorSelected = { floor ->
                    selectedFloor = floor
                    navController.navigate(Routes.FLOOR_PLAN)
                }
            )
        }
        composable(Routes.FLOOR_PLAN) {

            val floor = selectedFloor ?: return@composable

            val dashboardViewModel: DashboardViewModel = viewModel()

            val floors = dashboardViewModel.floors.collectAsState().value


            val floorPlanViewModel = remember(floor.id) {
                FloorPlanViewModel(floor.id)
            }


            FloorPlanScreen(
                floors = floors,
                selectedFloor = floor,
                floorPlanViewModel = floorPlanViewModel,

                onBack = {
                    navController.popBackStack()
                },

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
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
    }

}