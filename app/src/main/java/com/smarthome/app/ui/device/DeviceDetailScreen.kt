package com.smarthome.app.ui.device


import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color



import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.ui.theme.PrimaryBlue



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(device: Device){

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Device Details",
                        color = Color.White
                    )

                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            // later connect navigation back
                        }
                    ){

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )

                    }

                },

                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = PrimaryBlue

                )


            )

        }

    ){ paddingValues ->

        Column(modifier = Modifier.fillMaxSize().background(BackgroundLight).padding(paddingValues).padding(16.dp)) {

            Surface(

                modifier = Modifier.fillMaxSize().padding(8.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp

            ) {

                when (device.type) {

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

        }

    }

}