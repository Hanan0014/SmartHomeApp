package com.smarthome.app.ui.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import java.lang.System.currentTimeMillis
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.R


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


    Column(

        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Card(

            modifier = Modifier.fillMaxWidth(),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )

        ) {

            Column(

                modifier = Modifier.padding(20.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {


                Image(

                    painter = painterResource(
                        id = R.drawable.iron
                    ),

                    contentDescription = "Iron",

                    modifier = Modifier
                        .height(120.dp)

                )


                Spacer(
                    modifier = Modifier.height(16.dp)
                )


                Text(

                    text = device.name,

                    style = MaterialTheme.typography.headlineSmall

                )


                Text(

                    text = "Safety Scheduled Appliance",

                    style = MaterialTheme.typography.bodyMedium

                )


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

        ) {

            Column(

                modifier = Modifier.padding(20.dp)

            ) {

                Text(

                    text = "Safety Information",

                    style = MaterialTheme.typography.titleMedium

                )


                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                Text(

                    text =
                        "Maximum ON Duration: ${device.maxOnDurationSeconds ?: "Not configured"} seconds"

                )


                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                Text(

                    text =
                        if (remainingSeconds.value != null) {

                            "Remaining Time: ${formatTime(remainingSeconds.value!!)}"

                        } else {

                            "Remaining Time: Not available"

                        }

                )


                if (remainingSeconds.value == 0L) {

                    Text(
                        text = "Maximum ON duration reached"
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

        ) {

            Row(

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                Text(
                    text = "Power Control"
                )


                Switch(

                    checked = isOn.value,

                    onCheckedChange = {

                        isOn.value = it

                        // Firebase update later

                    }

                )

            }

        }

    }

}


fun formatTime(seconds: Long): String {

    val minutes = seconds / 60

    val remainingSeconds = seconds % 60

    return String.format("%02d:%02d",minutes,remainingSeconds)

}