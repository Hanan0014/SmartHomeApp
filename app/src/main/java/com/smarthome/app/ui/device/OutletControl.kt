package com.smarthome.app.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource


import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.ui.theme.StatusOn
import com.smarthome.app.ui.theme.StatusOff
import com.smarthome.app.R

@Composable
fun OutletControl(device: Device) {

    val isOn = remember {
        mutableStateOf(device.status == DeviceStatus.ON)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Outlet Control",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Column(

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {


                    Image(

                        painter = painterResource(
                            id = R.drawable.outlet
                        ),

                        contentDescription = "Outlet",

                        modifier = Modifier
                            .height(120.dp)

                    )


                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )


                    Row(

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Icon(
                            imageVector = Icons.Default.Power,
                            contentDescription = null,

                            tint =
                                if (isOn.value)
                                    StatusOn
                                else
                                    StatusOff
                        )


                        Spacer(
                            modifier = Modifier.padding(8.dp)
                        )


                        Column {

                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleLarge
                            )


                            Text(
                                text = "Power Outlet",
                                style = MaterialTheme.typography.bodyMedium
                            )

                        }

                    }

                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Status"
                )

                Text(
                    text = if (isOn.value) "ON" else "OFF",

                    color =
                        if (isOn.value)
                            StatusOn
                        else
                            StatusOff,

                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

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

}