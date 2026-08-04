package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.model.Room
import com.smarthome.app.data.model.SubSwitch
import com.smarthome.app.ui.theme.PrimaryBlue

private val SUB_SWITCH_COUNT_OPTIONS = listOf(2, 3, 5)
private const val DEFAULT_MAX_DURATION_SECONDS = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    floorId: String,
    room: Room,
    onBack: () -> Unit,
    onDeviceSelected: (Device) -> Unit
) {
    // Reuses FloorPlanViewModel (already observes every device + room on
    // this floor) rather than a new ViewModel per room — keeps a single
    // source of truth for the floor's data instead of duplicating listeners.
    val viewModel: FloorPlanViewModel = viewModel(
        key = floorId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                FloorPlanViewModel(floorId) as T
        }
    )
    val allDevices by viewModel.devices.collectAsState()
    val roomDevices = allDevices.filter { it.roomId == room.id }

    var showAddDeviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "${room.icon} ${room.name}", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDeviceDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add device to ${room.name}")
            }
        }
    ) { padding ->
        if (roomDevices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No devices in ${room.name} yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(roomDevices, key = { it.id }) { device ->
                    RoomDeviceRow(
                        device = device,
                        onToggle = { viewModel.toggleDevice(device) },
                        onClick = { onDeviceSelected(device) }
                    )
                }
            }
        }
    }

    if (showAddDeviceDialog) {
        AddDeviceToRoomDialog(
            onDismiss = { showAddDeviceDialog = false },
            onConfirm = { name, type, subSwitchCount, maxDurationSeconds ->
                val placement = viewModel.firstFreeCellInRoom(room)
                    ?: (room.cells.firstOrNull()?.split(",")?.let { it[0].toInt() to it[1].toInt() } ?: (0 to 0))
                viewModel.addDevice(
                    Device(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        gridX = placement.first,
                        gridY = placement.second,
                        roomId = room.id,
                        subSwitches = if (type == DeviceType.MULTI_SWITCH) {
                            (1..subSwitchCount).map { index ->
                                SubSwitch(index.toString(), "Switch $index", DeviceStatus.OFF)
                            }
                        } else emptyList(),
                        maxOnDurationSeconds = if (type == DeviceType.SCHEDULED_APPLIANCE) maxDurationSeconds else null
                    )
                )
                showAddDeviceDialog = false
            }
        )
    }
}

@Composable
private fun RoomDeviceRow(device: Device, onToggle: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(device.statusColor()))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${device.type.name.replace("_", " ")} • ${device.status.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (device.type != DeviceType.CAMERA) {
                Switch(
                    checked = device.status == DeviceStatus.ON,
                    enabled = device.status == DeviceStatus.ON || device.status == DeviceStatus.OFF,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceToRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: DeviceType, subSwitchCount: Int, maxDurationSeconds: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.OUTLET) }
    var typeExpanded by remember { mutableStateOf(false) }
    var subSwitchCount by remember { mutableStateOf(SUB_SWITCH_COUNT_OPTIONS.first()) }
    var subSwitchExpanded by remember { mutableStateOf(false) }
    var maxDurationText by remember { mutableStateOf(DEFAULT_MAX_DURATION_SECONDS.toString()) }

    val nameError = name.isNotEmpty() && name.isBlank()
    val maxDurationValue = maxDurationText.toLongOrNull()
    val maxDurationError = selectedType == DeviceType.SCHEDULED_APPLIANCE &&
            (maxDurationValue == null || maxDurationValue <= 0)
    val canSubmit = name.isNotBlank() && !maxDurationError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name can't be blank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Device Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        DeviceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace('_', ' ')) },
                                onClick = { selectedType = type; typeExpanded = false }
                            )
                        }
                    }
                }

                if (selectedType == DeviceType.MULTI_SWITCH) {
                    Spacer(Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = subSwitchExpanded,
                        onExpandedChange = { subSwitchExpanded = !subSwitchExpanded }
                    ) {
                        OutlinedTextField(
                            value = "$subSwitchCount switches",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sub-switch count") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subSwitchExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = subSwitchExpanded, onDismissRequest = { subSwitchExpanded = false }) {
                            SUB_SWITCH_COUNT_OPTIONS.forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count switches") },
                                    onClick = { subSwitchCount = count; subSwitchExpanded = false }
                                )
                            }
                        }
                    }
                }

                if (selectedType == DeviceType.SCHEDULED_APPLIANCE) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = maxDurationText,
                        onValueChange = { maxDurationText = it },
                        label = { Text("Max ON duration (seconds)") },
                        singleLine = true,
                        isError = maxDurationError,
                        supportingText = { if (maxDurationError) Text("Enter a positive number of seconds") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedType, subSwitchCount, maxDurationValue ?: DEFAULT_MAX_DURATION_SECONDS) },
                enabled = canSubmit
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}