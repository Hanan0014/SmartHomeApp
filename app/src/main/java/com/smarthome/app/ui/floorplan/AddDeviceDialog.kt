package com.smarthome.app.ui.floorplan

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smarthome.app.data.model.DeviceType


@Composable
fun AddDeviceDialog(
    gridX:Int, 
    gridY:Int, 
    onDismiss:() -> Unit, 
    onAddDevice:(String, DeviceType, Int, Int) -> Unit
){
    
    var deviceName by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf(DeviceType.OUTLET)
    }

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },
        
        title = {
            Text(
                text = "Add Device"
            )
        },

        text = {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)){

                OutlinedTextField(

                    value = deviceName,
                    onValueChange = {
                        deviceName = it
                    },
                    label = {
                        Text("Device Name")
                    },

                )

                Text(
                    text = "Device Type"
                )

                DeviceType.entries.forEach{ type ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){

                        RadioButton(

                            selected = selectedType == type,

                            onClick = {
                                selectedType = type
                            }

                        )


                        Text(
                            text = type.name
                        )

                    }

                }

            }

        },

        confirmButton = {

            Button(
                onClick = {

                    if (deviceName.isNotBlank()) {
                        onAddDevice(deviceName, selectedType, gridX, gridY)
                    }

                }
            ) {

                Text("Add")

            }

        },

        dismissButton = {

            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {

                Text("Cancel")

            }

        }


    )

}