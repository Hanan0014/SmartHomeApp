package com.smarthome.app.ui.device

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.Card
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp


import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus


@Composable
fun MultiSwitchControl(device: Device){

    Text(text = device.name)

    Text(text = "Number of Switches: ${device.subSwitches.size}")

    device.subSwitches.forEach { subSwitch ->

        val isOn = remember {

            mutableStateOf(subSwitch.status == DeviceStatus.ON)

        }

        Card() {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(text = subSwitch.label)

                    Switch(

                        checked = isOn.value,

                        onCheckedChange = {

                            isOn.value = it

                        }

                    )

                }


                Text(text = subSwitch.status.name)

                Spacer(modifier = Modifier.height(16.dp))

            }

        }

    }

}