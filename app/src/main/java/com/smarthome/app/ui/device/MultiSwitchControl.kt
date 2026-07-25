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
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp


import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.R
import com.smarthome.app.ui.theme.StatusOn
import com.smarthome.app.ui.theme.StatusOff


@Composable
fun MultiSwitchControl(device: Device){

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Image(

            painter = painterResource(
                id = R.drawable.switch_board
            ),

            contentDescription = "Switch Board",

            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        Text(

            text = device.name,

            style = MaterialTheme.typography.headlineSmall

        )


        Text(

            text = "Multi Switch Board",

            style = MaterialTheme.typography.bodyMedium

        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(

            text = "Total Switches: ${device.subSwitches.size}",

            style = MaterialTheme.typography.bodyLarge

        )


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

}