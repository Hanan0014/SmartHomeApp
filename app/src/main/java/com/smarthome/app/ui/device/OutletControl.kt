package com.smarthome.app.ui.device

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus


@Composable
fun OutletControl(device: Device){

    var isOn = remember {

        mutableStateOf(device.status == DeviceStatus.ON)

    }

    Text(text = device.name)
    Text(text = device.status.name)

    Switch(

        checked = isOn.value,

        onCheckedChange = {

            isOn.value = it

        }

    )

}