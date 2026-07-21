package com.smarthome.app.ui.device


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceType




@Composable
fun DeviceDetailScreen(device: Device){

    when(device.type){

        DeviceType.OUTLET -> {

            OutletControl(device)

        }

        DeviceType.MULTI_SWITCH -> {

            MultiSwitchControl(device)

        }

        DeviceType.SCHEDULED_APPLIANCE -> {

            ScheduledApplianceControl(device)

        }

        DeviceType.LIGHT_SCHEDULE -> {

            LightScheduleControl(device)

        }

        DeviceType.CAMERA -> {

            CameraControl(device)

        }

    }

}