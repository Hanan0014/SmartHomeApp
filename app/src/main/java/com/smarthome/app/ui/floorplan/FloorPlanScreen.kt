package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

import com.smarthome.app.ui.theme.PrimaryBlue
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.model.Room
import com.smarthome.app.data.model.ROOM_TYPE_PRESETS
import com.smarthome.app.data.model.SubSwitch
import com.smarthome.app.ui.theme.StatusDisconnected
import com.smarthome.app.ui.theme.StatusError
import com.smarthome.app.ui.theme.StatusOff
import com.smarthome.app.ui.theme.StatusOn

// A small fixed palette so each room gets a distinct, consistent tint —
// cycles if there are more rooms than colors.
private val ROOM_COLOR_PALETTE = listOf(
    Color(0xFFB3E5FC), Color(0xFFC8E6C9), Color(0xFFFFE0B2),
    Color(0xFFF8BBD0), Color(0xFFD1C4E9), Color(0xFFFFF9C4)
)

private fun roomColor(room: Room, allRooms: List<Room>): Color {
    val index = allRooms.indexOf(room).coerceAtLeast(0)
    return ROOM_COLOR_PALETTE[index % ROOM_COLOR_PALETTE.size]
}

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
    val rooms by viewModel.rooms.collectAsState()

    var showAddDeviceDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Room-definition mode: while active, tapping grid cells selects them
    // instead of opening a device/Add Device dialog.
    var isDefiningRoom by remember { mutableStateOf(false) }
    var selectedCells by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var showSaveRoomDialog by remember { mutableStateOf(false) }
    var roomFilter by remember { mutableStateOf<Room?>(null) } // room chip filter for device list

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = floor.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        isDefiningRoom = !isDefiningRoom
                        selectedCells = emptySet()
                    }) {
                        Icon(
                            imageVector = if (isDefiningRoom) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isDefiningRoom) "Cancel" else "Add Room",
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        floatingActionButton = {
            if (isDefiningRoom && selectedCells.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = { showSaveRoomDialog = true }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Room (${selectedCells.size} cells)")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            if (isDefiningRoom) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "Tap grid cells to select the area for a new room, then tap Save.",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val context = LocalContext.current
            val imageRes = remember(floor.planImageName) {
                context.resources.getIdentifier(floor.planImageName, "drawable", context.packageName)
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(floor.gridCols.toFloat() / floor.gridRows.toFloat())
                        .padding(12.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            alpha = 0.5f
                        )
                    }
                    Column(Modifier.fillMaxSize()) {
                        for (row in 0 until floor.gridRows) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until floor.gridCols) {
                                    val device = devices.firstOrNull { it.gridX == col && it.gridY == row }
                                    val cellRoom = rooms.firstOrNull { it.containsCell(col, row) }
                                    val isSelected = selectedCells.contains(col to row)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                    cellRoom != null -> roomColor(cellRoom, rooms).copy(alpha = 0.55f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                if (isDefiningRoom) {
                                                    selectedCells = if (isSelected) {
                                                        selectedCells - (col to row)
                                                    } else {
                                                        selectedCells + (col to row)
                                                    }
                                                } else if (device == null) {
                                                    showAddDeviceDialog = col to row
                                                } else {
                                                    onDeviceSelected(device)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        device?.let { DeviceGridTile(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Room chips — tap to filter the device list below to just that
            // room, tap again to clear the filter.
            if (rooms.isNotEmpty() && !isDefiningRoom) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rooms, key = { it.id }) { room ->
                        FilterChip(
                            selected = roomFilter?.id == room.id,
                            onClick = { roomFilter = if (roomFilter?.id == room.id) null else room },
                            label = { Text("${room.icon} ${room.name}") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider()

            val visibleDevices = if (roomFilter != null) {
                devices.filter { it.roomId == roomFilter!!.id }
            } else devices

            if (visibleDevices.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (roomFilter != null)
                            "No devices in ${roomFilter!!.name} yet."
                        else
                            "No devices yet. Tap an empty cell on the grid above to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumnDeviceList(visibleDevices, onToggle = { viewModel.toggleDevice(it) }, onSelect = onDeviceSelected)
            }
        }
    }

    if (showSaveRoomDialog) {
        SaveRoomDialog(
            onDismiss = { showSaveRoomDialog = false },
            onConfirm = { name, icon ->
                viewModel.addRoom(
                    Room(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        icon = icon,
                        cells = selectedCells.map { (x, y) -> Room.cellKey(x, y) }
                    )
                )
                showSaveRoomDialog = false
                isDefiningRoom = false
                selectedCells = emptySet()
            }
        )
    }

    showAddDeviceDialog?.let { (x, y) ->
        val autoRoom = viewModel.roomForCell(x, y)
        AddDeviceDialog(
            rooms = rooms,
            preselectedRoom = autoRoom,
            onDismiss = { showAddDeviceDialog = null },
            onConfirm = { name, type, subSwitchCount, maxDurationSeconds, roomId ->
                viewModel.addDevice(
                    Device(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        gridX = x,
                        gridY = y,
                        roomId = roomId,
                        subSwitches = if (type == DeviceType.MULTI_SWITCH) {
                            (1..subSwitchCount).map { index ->
                                SubSwitch(index.toString(), "Switch $index", DeviceStatus.OFF)
                            }
                        } else emptyList(),
                        maxOnDurationSeconds = if (type == DeviceType.SCHEDULED_APPLIANCE) maxDurationSeconds else null
                    )
                )
                showAddDeviceDialog = null
            }
        )
    }
}

@Composable
private fun DeviceGridTile(device: Device) {
    val color = device.statusColor()
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveRoomDialog(onDismiss: () -> Unit, onConfirm: (name: String, icon: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(ROOM_TYPE_PRESETS.first().second) }
    val nameError = name.isNotEmpty() && name.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name This Room") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room name") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name can't be blank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ROOM_TYPE_PRESETS) { (presetName, presetIcon) ->
                        val isSelected = selectedIcon == presetIcon
                        AssistChip(
                            onClick = {
                                selectedIcon = presetIcon
                                if (name.isBlank()) name = presetName
                            },
                            label = { Text("$presetIcon $presetName") },
                            colors = if (isSelected) AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedIcon) },
                enabled = name.isNotBlank()
            ) { Text("Save Room") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val SUB_SWITCH_COUNT_OPTIONS = listOf(2, 3, 5)
private const val DEFAULT_MAX_DURATION_SECONDS = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceDialog(
    rooms: List<Room>,
    preselectedRoom: Room?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: DeviceType, subSwitchCount: Int, maxDurationSeconds: Long, roomId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.OUTLET) }
    var typeExpanded by remember { mutableStateOf(false) }
    var subSwitchCount by remember { mutableStateOf(SUB_SWITCH_COUNT_OPTIONS.first()) }
    var subSwitchExpanded by remember { mutableStateOf(false) }
    var maxDurationText by remember { mutableStateOf(DEFAULT_MAX_DURATION_SECONDS.toString()) }
    var selectedRoom by remember { mutableStateOf(preselectedRoom) }
    var roomExpanded by remember { mutableStateOf(false) }

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

                if (rooms.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = roomExpanded,
                        onExpandedChange = { roomExpanded = !roomExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRoom?.let { "${it.icon} ${it.name}" } ?: "No room (unassigned)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Room") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("No room (unassigned)") },
                                onClick = { selectedRoom = null; roomExpanded = false }
                            )
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text("${room.icon} ${room.name}") },
                                    onClick = { selectedRoom = room; roomExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

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
                onClick = {
                    onConfirm(name, selectedType, subSwitchCount, maxDurationValue ?: DEFAULT_MAX_DURATION_SECONDS, selectedRoom?.id)
                },
                enabled = canSubmit
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LazyColumnDeviceList(
    devices: List<Device>,
    onToggle: (Device) -> Unit,
    onSelect: (Device) -> Unit
) {
    LazyColumn(
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

private fun Device.statusColor(): Color = when (status) {
    DeviceStatus.ON -> StatusOn
    DeviceStatus.OFF -> StatusOff
    DeviceStatus.ERROR -> StatusError
    DeviceStatus.DISCONNECTED -> StatusDisconnected
}