package com.smarthome.app.ui.device


import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

import com.smarthome.app.data.model.Device


@Composable
fun LightScheduleControl(device: Device){

    Text(text = device.name)

}