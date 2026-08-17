package com.smarthome.app.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarthome.app.data.model.Device
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

private fun isValidTime(value: String): Boolean =
    try {
        LocalTime.parse(value, TIME_FORMATTER)
        true
    } catch (e: DateTimeParseException) {
        false
    }

private fun nextTransitionText(start: String?, end: String?, enabled: Boolean): String {
    if (!enabled || start == null || end == null || !isValidTime(start) || !isValidTime(end)) {
        return "Schedule not active"
    }
    val now = LocalTime.now()
    val startTime = LocalTime.parse(start, TIME_FORMATTER)
    val endTime = LocalTime.parse(end, TIME_FORMATTER)

    return if (now.isBefore(startTime)) {
        "Turns on at $start"
    } else if (now.isBefore(endTime) || endTime.isBefore(startTime)) {
        "Turns off at $end"
    } else {
        "Turns on at $start"
    }
}

@Composable
fun LightScheduleControl(
    device: Device,
    onSaveSchedule: (start: String, end: String, enabled: Boolean) -> Unit
) {
    var startInput by remember(device.id) { mutableStateOf(device.scheduleStart ?: "18:00") }
    var endInput by remember(device.id) { mutableStateOf(device.scheduleEnd ?: "06:00") }
    var enabledInput by remember(device.id) { mutableStateOf(device.scheduleEnabled) }

    val startValid = isValidTime(startInput)
    val endValid = isValidTime(endInput)

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "Light Schedule", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = device.name, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = nextTransitionText(device.scheduleStart, device.scheduleEnd, device.scheduleEnabled),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Schedule Enabled")
                    Switch(checked = enabledInput, onCheckedChange = { enabledInput = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = startInput,
                    onValueChange = { startInput = it },
                    label = { Text("On time (HH:mm)") },
                    isError = !startValid,
                    supportingText = { if (!startValid) Text("Use 24-hour HH:mm, e.g. 18:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = endInput,
                    onValueChange = { endInput = it },
                    label = { Text("Off time (HH:mm)") },
                    isError = !endValid,
                    supportingText = { if (!endValid) Text("Use 24-hour HH:mm, e.g. 06:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSaveSchedule(startInput, endInput, enabledInput) },
                    enabled = startValid && endValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Schedule")
                }
            }
        }
    }
}