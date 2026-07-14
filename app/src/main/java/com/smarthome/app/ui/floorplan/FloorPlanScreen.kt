package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.model.Floor
import com.smarthome.app.ui.theme.StatusDisconnected
import com.smarthome.app.ui.theme.StatusError
import com.smarthome.app.ui.theme.StatusOff
import com.smarthome.app.ui.theme.StatusOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    floor: Floor,
    onBack: () -> Unit,
    onDeviceSelected: (Device) -> Unit
) {
    val viewModel: FloorPlanViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                FloorPlanViewModel(floor.id) as T
        }
    )
    val devices by viewModel.devices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(floor.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Abstract grid overlay representing the floor plan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(floor.gridCols.toFloat() / floor.gridRows.toFloat())
                    .padding(12.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            ) {
                Column(Modifier.fillMaxSize()) {
                    for (row in 0 until floor.gridRows) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            for (col in 0 until floor.gridCols) {
                                val device = devices.firstOrNull { it.gridX == col && it.gridY == row }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    device?.let {
                                        DeviceGridTile(it, onClick = { onDeviceSelected(it) })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Device list (easier interaction than tapping tiny grid cells)
            LazyColumnDeviceList(devices, onToggle = { viewModel.toggleDevice(it) }, onSelect = onDeviceSelected)
        }
    }
}

@Composable
private fun DeviceGridTile(device: Device, onClick: () -> Unit) {
    val color = device.statusColor()
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun LazyColumnDeviceList(
    devices: List<Device>,
    onToggle: (Device) -> Unit,
    onSelect: (Device) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceRow(device, onToggle = { onToggle(device) }, onClick = { onSelect(device) })
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onToggle: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape).background(device.statusColor())
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${device.type.name.replace('_', ' ')} · ${device.status.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (device.type != DeviceType.CAMERA) {
                Switch(
                    checked = device.status == DeviceStatus.ON,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

private fun Device.statusColor(): Color = when (status) {
    DeviceStatus.ON -> StatusOn
    DeviceStatus.OFF -> StatusOff
    DeviceStatus.ERROR -> StatusError
    DeviceStatus.DISCONNECTED -> StatusDisconnected
}
