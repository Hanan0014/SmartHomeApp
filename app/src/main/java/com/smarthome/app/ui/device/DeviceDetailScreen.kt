package com.smarthome.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.ui.components.SmartHomeBackground
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(floorId: String, device: Device, onBack: () -> Unit) {

    // device.id is stable even though the object itself is a point-in-time
    // snapshot from the floor plan list — the ViewModel below takes over
    // observing live state keyed on that id.
    val viewModel: DeviceDetailViewModel = viewModel(
        key = device.id,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                DeviceDetailViewModel(floorId, device.id) as T
        }
    )

    // Live device from Firebase; falls back to the snapshot passed in until
    // the first Firebase read lands, so the screen never shows blank.
    val liveDevice by viewModel.device.collectAsState()
    val currentDevice = liveDevice ?: device


    SmartHomeBackground {

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDevice.name,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    //.background(BackgroundLight)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (liveDevice == null) {
                    // Device was deleted from Firebase (e.g. floor cleanup) while
                    // this screen was open — show that clearly instead of a stale
                    // control that silently does nothing when toggled.
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This device is no longer available.")
                    }
                    return@Column
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp
                ) {
                    when (currentDevice.type) {
                        DeviceType.OUTLET -> OutletControl(
                            device = currentDevice,
                            onToggle = { viewModel.toggleDevice() }
                        )
                        DeviceType.MULTI_SWITCH -> MultiSwitchControl(
                            device = currentDevice,
                            onToggleSubSwitch = { id, status -> viewModel.toggleSubSwitch(id, status) }
                        )
                        DeviceType.SCHEDULED_APPLIANCE -> ScheduledApplianceControl(
                            device = currentDevice,
                            onToggle = { viewModel.toggleDevice() }
                        )
                        DeviceType.LIGHT_SCHEDULE -> LightScheduleControl(
                            device = currentDevice,
                            onSaveSchedule = { start, end, enabled -> viewModel.updateSchedule(start, end, enabled) }
                        )
                        DeviceType.CAMERA -> CameraControl(device = currentDevice)
                    }
                }
            }
        }

    }

}