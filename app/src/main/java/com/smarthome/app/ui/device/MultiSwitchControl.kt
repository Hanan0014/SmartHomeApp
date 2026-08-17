package com.smarthome.app.ui.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smarthome.app.R
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.SubSwitch

@Composable
fun MultiSwitchControl(
    device: Device,
    onToggleSubSwitch: (subSwitchId: String, newStatus: DeviceStatus) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.switch_board),
            contentDescription = "Switch Board",
            modifier = Modifier.height(120.dp).fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = device.name, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Multi Switch Board", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (device.subSwitches.isEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "No sub-switches configured for this unit.")
            return
        }

        Text(text = "Total Switches: ${device.subSwitches.size}", style = MaterialTheme.typography.bodyLarge)

        device.subSwitches.forEach { subSwitch ->
            SubSwitchRow(subSwitch = subSwitch, onToggle = { newStatus ->
                onToggleSubSwitch(subSwitch.id, newStatus)
            })
        }
    }
}

@Composable
private fun SubSwitchRow(subSwitch: SubSwitch, onToggle: (DeviceStatus) -> Unit) {
    val isOn = subSwitch.status == DeviceStatus.ON

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = subSwitch.label)
                Switch(
                    checked = isOn,
                    onCheckedChange = { checked ->
                        onToggle(if (checked) DeviceStatus.ON else DeviceStatus.OFF)
                    }
                )
            }
            Text(text = subSwitch.status.name)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}