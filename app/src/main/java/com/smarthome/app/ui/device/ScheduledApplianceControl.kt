package com.smarthome.app.ui.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smarthome.app.R
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import kotlinx.coroutines.delay

// Warn once less than this many seconds remain, not only after cutoff hits —
// this was the specific Phase 4 checklist item that was missing.
private const val WARNING_THRESHOLD_SECONDS = 60L

@Composable
fun ScheduledApplianceControl(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON
    var remainingSeconds by remember(device.id) { mutableStateOf<Long?>(null) }

    LaunchedEffect(device.id, device.turnedOnAtEpochMs, device.maxOnDurationSeconds) {
        while (true) {
            val maxDuration = device.maxOnDurationSeconds
            val turnedOnAt = device.turnedOnAtEpochMs
            if (maxDuration != null && turnedOnAt != null && isOn) {
                val elapsedSeconds = (System.currentTimeMillis() - turnedOnAt) / 1000
                remainingSeconds = maxOf(0, maxDuration - elapsedSeconds)
            } else {
                remainingSeconds = null
            }
            delay(1000)
        }
    }

    val isNearCutoff = remainingSeconds != null && remainingSeconds!! in 1..WARNING_THRESHOLD_SECONDS
    val isCutoff = remainingSeconds == 0L

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.scheduled_appliance),
                    contentDescription = "Scheduled Appliance",
                    modifier = Modifier.height(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = device.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = "Safety Scheduled Appliance", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNearCutoff || isCutoff)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Safety Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Maximum ON Duration: ${device.maxOnDurationSeconds ?: "Not configured"} seconds")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (remainingSeconds != null) {
                        "Remaining Time: ${formatTime(remainingSeconds!!)}"
                    } else {
                        "Remaining Time: Not applicable"
                    }
                )

                if (isNearCutoff) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ Approaching maximum ON duration — will auto shut off soon.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isCutoff) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ Maximum ON duration reached — device was automatically shut off.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Power Control")
                Switch(
                    checked = isOn,
                    enabled = device.status == DeviceStatus.ON || device.status == DeviceStatus.OFF,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}