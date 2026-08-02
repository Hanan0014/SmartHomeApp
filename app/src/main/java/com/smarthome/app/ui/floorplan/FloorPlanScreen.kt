package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.smarthome.app.data.model.SubSwitch
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
    var showAddDeviceDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = floor.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
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
                        // Grid coordinates are generated from these loop bounds
                        // (0 until gridCols/gridRows), so every tap position is
                        // structurally guaranteed to be in-range — no separate
                        // bounds validation is needed for device placement.
                        for (row in 0 until floor.gridRows) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until floor.gridCols) {
                                    val device = devices.firstOrNull { it.gridX == col && it.gridY == row }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                                            .clickable {
                                                if (device == null) {
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

            HorizontalDivider()

            if (devices.isEmpty()) {
                // Phase 7 empty state: a floor with zero devices.
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No devices yet. Tap an empty cell on the grid above to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumnDeviceList(devices, onToggle = { viewModel.toggleDevice(it) }, onSelect = onDeviceSelected)
            }
        }
    }

    showAddDeviceDialog?.let { (x, y) ->
        AddDeviceDialog(
            onDismiss = { showAddDeviceDialog = null },
            onConfirm = { name, type, subSwitchCount, maxDurationSeconds ->
                viewModel.addDevice(
                    Device(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        gridX = x,
                        gridY = y,
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

// Spec requires a variable sub-switch count: 2, 3, or 5.
private val SUB_SWITCH_COUNT_OPTIONS = listOf(2, 3, 5)
private const val DEFAULT_MAX_DURATION_SECONDS = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: DeviceType, subSwitchCount: Int, maxDurationSeconds: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.OUTLET) }
    var typeExpanded by remember { mutableStateOf(false) }
    var subSwitchCount by remember { mutableStateOf(SUB_SWITCH_COUNT_OPTIONS.first()) }
    var subSwitchExpanded by remember { mutableStateOf(false) }
    var maxDurationText by remember { mutableStateOf(DEFAULT_MAX_DURATION_SECONDS.toString()) }

    // Phase 7: no blank names allowed.
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
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DeviceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace('_', ' ')) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Type-specific initial fields, per Phase 3's checklist.
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
                        ExposedDropdownMenu(
                            expanded = subSwitchExpanded,
                            onDismissRequest = { subSwitchExpanded = false }
                        ) {
                            SUB_SWITCH_COUNT_OPTIONS.forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count switches") },
                                    onClick = {
                                        subSwitchCount = count
                                        subSwitchExpanded = false
                                    }
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
                        supportingText = {
                            if (maxDurationError) Text("Enter a positive number of seconds")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        selectedType,
                        subSwitchCount,
                        maxDurationValue ?: DEFAULT_MAX_DURATION_SECONDS
                    )
                },
                enabled = canSubmit
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
private fun DeviceRow(
    device: Device,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(device.statusColor())
            )
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
                    // Phase 7: don't let the user toggle a device that's
                    // reporting ERROR/DISCONNECTED — there's nothing real on
                    // the other end to actually respond to the toggle.
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