package com.smarthome.app.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smarthome.app.data.model.Device

@Composable
fun CameraControl(device: Device) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = device.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val snapshotUrl = device.snapshotUrl
                if (!snapshotUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = snapshotUrl,
                        contentDescription = "Camera snapshot for ${device.name}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No stream configured")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Device Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Type: Security Camera")
                Text(
                    text = if (device.snapshotUrl.isNullOrBlank())
                        "Stream: Not configured"
                    else
                        "Stream: Mock snapshot"
                )
            }
        }
    }
}