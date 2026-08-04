package com.smarthome.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
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
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 17.dp)
                ) {
                    DeviceHeader(
                        device = currentDevice,
                        onBack = onBack
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {

                    DeviceMetaChips(
                        device = currentDevice
                    )

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





@Composable 
fun DeviceHeader(device: Device, onBack: () -> Unit){

    Column{

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable {
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )

            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = device.type.name,
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = device.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )

            }

            DeviceStatusBadge(status = device.status.name)

        }

    }

}

@Composable
fun DeviceStatusBadge(status: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusColor(status).copy(alpha = 0.15f))
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor(status))
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = status,
                color = statusColor(status),
                style = MaterialTheme.typography.labelMedium
            )

        }

    }

}

fun statusColor(status: String): Color {

    return when (status.uppercase()) {

        "ON" -> Color(0xFF22C55E)       // Green
        "OFF" -> Color(0xFF94A3B8)      // Grey
        "ACTIVE" -> Color(0xFF3B82F6)  // Blue

        else -> Color(0xFFCBD5E1)

    }

}