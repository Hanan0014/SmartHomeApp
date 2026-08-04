package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

import com.smarthome.app.ui.theme.PrimaryBlue
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.model.Room
import com.smarthome.app.data.model.ROOM_TYPE_PRESETS

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
    onDeviceSelected: (Device) -> Unit,
    onRoomSelected: (Room) -> Unit
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

    // Room-definition mode: while active, tapping grid cells selects them
    // instead of navigating into a room.
    var isDefiningRoom by remember { mutableStateOf(false) }
    var selectedCells by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var showSaveRoomDialog by remember { mutableStateOf(false) }

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
            } else if (rooms.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "Tap \"Add Room\" above to mark out a Hall, Kitchen, or other space on the grid — devices live inside rooms.",
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
                                                when {
                                                    isDefiningRoom -> {
                                                        selectedCells = if (isSelected) {
                                                            selectedCells - (col to row)
                                                        } else {
                                                            selectedCells + (col to row)
                                                        }
                                                    }
                                                    device != null -> onDeviceSelected(device)
                                                    cellRoom != null -> onRoomSelected(cellRoom)
                                                    // Tapping an empty cell outside any room does
                                                    // nothing — devices must belong to a room now,
                                                    // so there's nothing meaningful to open here.
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

            HorizontalDivider()

            // Rooms list replaces the old flat device list — devices now
            // live inside rooms, so browsing by room is the primary path.
            if (rooms.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No rooms yet. Use \"Add Room\" above to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rooms, key = { it.id }) { room ->
                        val deviceCount = devices.count { it.roomId == room.id }
                        RoomListCard(
                            room = room,
                            deviceCount = deviceCount,
                            color = roomColor(room, rooms),
                            onClick = { onRoomSelected(room) }
                        )
                    }
                }
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
}

@Composable
private fun DeviceGridTile(device: Device) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(device.statusColor())
            .border(2.dp, Color.White, CircleShape)
    )
}

@Composable
private fun RoomListCard(
    room: Room,
    deviceCount: Int,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(room.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (deviceCount == 0) "No devices yet" else "$deviceCount device${if (deviceCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open ${room.name}")
        }
    }
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
                Spacer(Modifier.height(16.dp))
                Text("Room type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))

                ROOM_TYPE_PRESETS.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (presetName, presetIcon) ->
                            val isSelected = selectedIcon == presetIcon
                            RoomTypeCard(
                                icon = presetIcon,
                                label = presetName,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedIcon = presetIcon
                                    if (name.isBlank() || ROOM_TYPE_PRESETS.any { it.first == name }) {
                                        name = presetName
                                    }
                                }
                            )
                        }
                        repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
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

@Composable
private fun RoomTypeCard(
    icon: String,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

internal fun Device.statusColor(): Color = when (status) {
    DeviceStatus.ON -> com.smarthome.app.ui.theme.StatusOn
    DeviceStatus.OFF -> com.smarthome.app.ui.theme.StatusOff
    DeviceStatus.ERROR -> com.smarthome.app.ui.theme.StatusError
    DeviceStatus.DISCONNECTED -> com.smarthome.app.ui.theme.StatusDisconnected
}