package com.smarthome.app.ui.device

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import java.lang.System.currentTimeMillis
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus


@Composable
fun ScheduledApplianceControl(device: Device){

    var isOn = remember {

        mutableStateOf(device.status == DeviceStatus.ON)

    }

    var remainingSeconds = remember {

        mutableStateOf<Long?>(null)

    }


    LaunchedEffect(device.id) {

        while(true){

            if(device.maxOnDurationSeconds != null && device.turnedOnAtEpochMs != null){

                val elapsedSeconds = (currentTimeMillis() - device.turnedOnAtEpochMs) / 1000

                remainingSeconds.value = maxOf(0,device.maxOnDurationSeconds - elapsedSeconds)

                if(remainingSeconds.value == 0L){

                    isOn.value = false

                }

            }

            delay(1000)

        }

    }


    Text(text = device.name)
    Text(text = device.status.name)

    Text(
        text = if(device.maxOnDurationSeconds != null){

            "Maximum ON Duration: ${device.maxOnDurationSeconds} seconds"

        }else{

            "Maximum ON Duration: Not configured"

        }
    )

    Text(

        text =
            if(remainingSeconds.value != null){

                "Remaining Time: ${formatTime(remainingSeconds.value!!)}"

            }else{

                "Remaining Time: Not available"

            }

    )


    if(remainingSeconds.value == 0L){

        Text(
            text = "Maximum ON duration reached"
        )

    }


    Switch(

        checked = isOn.value,

        onCheckedChange = {

            isOn.value = it


            //**********we should update firebase here***************

        }

    )

}


fun formatTime(seconds: Long): String {

    val minutes = seconds / 60

    val remainingSeconds = seconds % 60

    return String.format("%02d:%02d",minutes,remainingSeconds)

}