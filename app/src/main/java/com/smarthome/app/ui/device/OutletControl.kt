package com.smarthome.app.ui.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smarthome.app.R
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.ui.theme.StatusOff
import com.smarthome.app.ui.theme.StatusOn

@Composable
fun OutletControl(device: Device, onToggle: () -> Unit) {
    // Reads status straight from the live `device` passed down from
    // DeviceDetailScreen — no local mutable state, so a toggle from another
    // phone or the backend safety worker shows up here immediately too.
    val isOn = device.status == DeviceStatus.ON

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "Outlet Control", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.outlet),
                        contentDescription = "Outlet",
                        modifier = Modifier.height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Power,
                            contentDescription = null,
                            tint = if (isOn) StatusOn else StatusOff
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Column {
                            Text(text = device.name, style = MaterialTheme.typography.titleLarge)
                            Text(text = "Power Outlet", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Status")
                Text(
                    text = when (device.status) {
                        DeviceStatus.ON -> "ON"
                        DeviceStatus.OFF -> "OFF"
                        DeviceStatus.ERROR -> "ERROR"
                        DeviceStatus.DISCONNECTED -> "DISCONNECTED"
                    },
                    color = if (isOn) StatusOn else StatusOff,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
<<<<<<< HEAD

                    Text(
                        text = "Power"
                    )

                    Switch(
                        checked = isOn.value,
                        onCheckedChange = {
                            isOn.value = it
                        }
                    )

                }

            }

        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        Card(

            modifier = Modifier.fillMaxWidth(),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )

        ){

            Column(

                modifier = Modifier.padding(20.dp)

            ){

                Text(

                    text = "Device Information",

                    style = MaterialTheme.typography.titleMedium

                )


                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                Text(
                    text = "Type: Electrical Outlet"
                )


                Text(
                    text = "Location: Living Room"
                )


                Text(
                    text = "Connection: Online"
                )

            }

        }

    }

=======
                    Text(text = "Power")
                    Switch(
                        // Toggle is disabled if the device is reporting ERROR or
                        // DISCONNECTED — matches Phase 7's "display distinctly" goal;
                        // there's no point letting the user flip a switch that isn't
                        // actually connected.
                        enabled = device.status == DeviceStatus.ON || device.status == DeviceStatus.OFF,
                        checked = isOn,
                        onCheckedChange = { onToggle() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Device Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Type: Electrical Outlet")
                Text(
                    text = "Connection: ${
                        if (device.status == DeviceStatus.DISCONNECTED) "Offline" else "Online"
                    }"
                )
            }
        }
    }
>>>>>>> 1a0fb62b9d0fdb8cd3b99f7e879d29cfd8c93baa
}