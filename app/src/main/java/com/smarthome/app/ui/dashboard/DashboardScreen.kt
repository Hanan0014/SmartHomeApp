package com.smarthome.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.smarthome.app.data.model.Floor
import com.smarthome.app.ui.components.ScrollableScreen
import com.smarthome.app.ui.dashboard.components.DashboardHeader
import com.smarthome.app.ui.components.SmartHomeBackground
import com.smarthome.app.ui.theme.PrimaryCyan
import com.smarthome.app.ui.theme.TextPrimary
import com.smarthome.app.ui.dashboard.components.FloorCard
import com.smarthome.app.data.model.DeviceStatus
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(

    onFloorSelected: (Floor) -> Unit,
    viewModel: DashboardViewModel = viewModel()

) {

    val floors by viewModel.floors.collectAsState()


    LaunchedEffect(floors) {
        println("Floors: $floors")
    }


    var showAddDialog by remember { mutableStateOf(false) }
    var floorPendingRename by remember { mutableStateOf<Floor?>(null) }
    var floorPendingDelete by remember { mutableStateOf<Floor?>(null) }

    SmartHomeBackground {

        Scaffold(

            containerColor = androidx.compose.ui.graphics.Color.Transparent,

            floatingActionButton = {

                FloatingActionButton(

                    onClick = {
                        showAddDialog = true
                    },
                    containerColor = PrimaryCyan

                ){

                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add floor"
                    )

                }

            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {


                DashboardHeader(

                    unreadCount = 3,
                    onNotificationClick = {

                    }

                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                Text(
                    text = "Floors",
                    color = TextPrimary,
                    fontSize = 18.sp
                )


                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                floors.forEach { floor ->

                    val deviceCount = floor.devices.size

                    val activeDevices = floor.devices.values.count { device ->
                        device.status == DeviceStatus.ON
                    }


                    FloorCard(

                        floor = floor,

                        deviceCount = deviceCount,

                        activeDevices = activeDevices,

                        onClick = {
                            onFloorSelected(floor)
                        }

                    )

                }

            }



        }

    }



    if (showAddDialog) {
        AddFloorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addFloor(
                    Floor(id = UUID.randomUUID().toString(), name = name, order = floors.size)
                )
                showAddDialog = false
            }
        )
    }

    floorPendingRename?.let { floor ->
        RenameFloorDialog(
            currentName = floor.name,
            onDismiss = { floorPendingRename = null },
            onConfirm = { newName ->
                viewModel.renameFloor(floor.id, newName)
                floorPendingRename = null
            }
        )
    }

    floorPendingDelete?.let { floor ->
        AlertDialog(
            onDismissRequest = { floorPendingDelete = null },
            title = { Text("Delete \"${floor.name}\"?") },
            text = { Text("This removes the floor and every device on it. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFloor(floor.id)
                    floorPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { floorPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}


@Composable
private fun AddFloorDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Floor Plan") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Floor name, e.g. Ground Floor") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameFloorDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Floor") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Floor name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}