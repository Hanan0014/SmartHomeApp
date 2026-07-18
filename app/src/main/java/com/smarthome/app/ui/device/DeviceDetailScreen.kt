package com.smarthome.app.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    floorId: String,
    initialDevice: Device,
    onBack: () -> Unit,
    repository: SmartHomeRepository = remember { SmartHomeRepository() }
) {
    val scope = rememberCoroutineScope()
    val device by repository.observeDevice(floorId, initialDevice.id)
        .collectAsState(initial = initialDevice)

    val currentDevice = device ?: initialDevice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDevice.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            when (currentDevice.type) {
                DeviceType.MULTI_SWITCH -> MultiSwitchPanel(floorId, currentDevice, repository, scope)
                DeviceType.SCHEDULED_APPLIANCE -> ScheduledAppliancePanel(floorId, currentDevice, repository, scope)
                DeviceType.LIGHT_SCHEDULE -> LightSchedulePanel(floorId, currentDevice, repository, scope)
                DeviceType.CAMERA -> CameraPanel(currentDevice)
                DeviceType.OUTLET -> OutletPanel(floorId, currentDevice, repository, scope)
            }
        }
    }
}

@Composable
private fun OutletPanel(floorId: String, device: Device, repo: SmartHomeRepository, scope: kotlinx.coroutines.CoroutineScope) {
    Text("Status: ${device.status}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    Button(onClick = { scope.launch { repo.toggleDevice(floorId, device) } }) {
        Text(if (device.status == DeviceStatus.ON) "Turn Off" else "Turn On")
    }
}

@Composable
private fun MultiSwitchPanel(floorId: String, device: Device, repo: SmartHomeRepository, scope: kotlinx.coroutines.CoroutineScope) {
    Text("Gang-box unit — ${device.subSwitches.size} switches", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(device.subSwitches, key = { it.id }) { sub ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sub.label)
                    Switch(
                        checked = sub.status == DeviceStatus.ON,
                        onCheckedChange = { checked ->
                            scope.launch {
                                repo.toggleSubSwitch(
                                    floorId, device.id, sub.id,
                                    if (checked) DeviceStatus.ON else DeviceStatus.OFF
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledAppliancePanel(floorId: String, device: Device, repo: SmartHomeRepository, scope: kotlinx.coroutines.CoroutineScope) {
    var timeLeft by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(device.status, device.turnedOnAtEpochMs) {
        if (device.status == DeviceStatus.ON && device.turnedOnAtEpochMs != null && device.maxOnDurationSeconds != null) {
            while (true) {
                val elapsed = (System.currentTimeMillis() - device.turnedOnAtEpochMs) / 1000
                val remaining = device.maxOnDurationSeconds - elapsed
                timeLeft = if (remaining > 0) remaining else 0
                kotlinx.coroutines.delay(1000)
            }
        } else {
            timeLeft = null
        }
    }

    Text("Fire-hazard appliance", style = MaterialTheme.typography.titleMedium)
    Text("Status: ${device.status}")
    if (timeLeft != null) {
        Text("Safety cutoff in: ${timeLeft!! / 60}m ${timeLeft!! % 60}s", color = MaterialTheme.colorScheme.primary)
    }
    Text("Max on-duration: ${device.maxOnDurationSeconds?.div(60) ?: "-"} min")
    Spacer(Modifier.height(8.dp))
    Text(
        "This device is protected by a server-side safety cutoff. If it stays ON " +
            "past its max duration, the backend will automatically turn it off and alert you.",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = { scope.launch { repo.toggleDevice(floorId, device) } }) {
        Text(if (device.status == DeviceStatus.ON) "Turn Off" else "Turn On")
    }
}

@Composable
private fun LightSchedulePanel(floorId: String, device: Device, repo: SmartHomeRepository, scope: kotlinx.coroutines.CoroutineScope) {
    var start by remember { mutableStateOf(device.scheduleStart ?: "18:00") }
    var end by remember { mutableStateOf(device.scheduleEnd ?: "23:00") }
    var enabled by remember { mutableStateOf(device.scheduleEnabled) }

    LaunchedEffect(device.id) {
        start = device.scheduleStart ?: "18:00"
        end = device.scheduleEnd ?: "23:00"
        enabled = device.scheduleEnabled
    }

    var error by remember { mutableStateOf<String?>(null) }

    Text("Automatic schedule", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = start,
        onValueChange = { start = it; error = null },
        label = { Text("Start (HH:mm)") },
        isError = error != null
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = end,
        onValueChange = { end = it; error = null },
        label = { Text("End (HH:mm)") },
        isError = error != null
    )
    if (error != null) {
        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = enabled, onCheckedChange = { enabled = it })
        Spacer(Modifier.width(8.dp))
        Text("Enabled")
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = {
        if (isValidTime(start) && isValidTime(end)) {
            scope.launch { repo.updateSchedule(floorId, device.id, start, end, enabled) }
        } else {
            error = "Please enter valid times (e.g. 09:00, 22:30)"
        }
    }) {
        Text("Save Schedule")
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = { scope.launch { repo.toggleDevice(floorId, device) } }) {
        Text(if (device.status == DeviceStatus.ON) "Turn Off Now" else "Turn On Now")
    }
}

private fun isValidTime(time: String): Boolean {
    val regex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
    return regex.matches(time)
}

@Composable
private fun CameraPanel(device: Device) {
    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(12.dp))
    Text("Mock camera feed", style = MaterialTheme.typography.titleMedium)
    Text("Snapshot URL: ${device.snapshotUrl ?: "not configured"}", style = MaterialTheme.typography.bodySmall)
    Text("Stream URI: ${device.streamUri ?: "not configured"}", style = MaterialTheme.typography.bodySmall)
}
