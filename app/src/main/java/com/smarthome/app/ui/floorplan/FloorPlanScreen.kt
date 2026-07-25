package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID



import com.smarthome.app.ui.theme.PrimaryBlue
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.model.SubSwitch
import com.smarthome.app.ui.theme.StatusDisconnected
import com.smarthome.app.ui.theme.StatusError
import com.smarthome.app.ui.theme.StatusOff
import com.smarthome.app.ui.theme.StatusOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    floor: Floor,
    onBack: () -> Unit,
    onDeviceSelected: (Device) -> Unit
) {
    val viewModel: FloorPlanViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                FloorPlanViewModel(floor.id) as T
        }
    )
    val devices by viewModel.devices.collectAsState()
    var showAddDeviceDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(

                title = {

                    Text(
                        text = floor.name,
                        color = Color.White
                    )

                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
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
    ) { padding ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)

        ) {


            // Abstract grid overlay representing the floor plan
            val context = LocalContext.current
            val imageRes = remember(floor.planImageName) {
                context.resources.getIdentifier(floor.planImageName, "drawable", context.packageName)
            }



            ElevatedCard(

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                shape = MaterialTheme.shapes.large,

                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 6.dp
                )

            ){

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(floor.gridCols.toFloat() / floor.gridRows.toFloat())
                        .padding(12.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            alpha = 0.5f
                        )
                    }

                    Column(Modifier.fillMaxSize()) {
                        for (row in 0 until floor.gridRows) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until floor.gridCols) {
                                    val device = devices.firstOrNull { it.gridX == col && it.gridY == row }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                                            .clickable {
                                                if (device == null) {
                                                    showAddDeviceDialog = col to row
                                                } else {
                                                    onDeviceSelected(device)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        device?.let {
                                            DeviceGridTile(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

            HorizontalDivider()

            // Device list (easier interaction than tapping tiny grid cells)
            LazyColumnDeviceList(devices, onToggle = { viewModel.toggleDevice(it) }, onSelect = onDeviceSelected)
        }
    }

    showAddDeviceDialog?.let { (x, y) ->
        AddDeviceDialog(
            onDismiss = { showAddDeviceDialog = null },
            onConfirm = { name, type ->
                viewModel.addDevice(
                    Device(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        gridX = x,
                        gridY = y,
                        subSwitches = if (type == DeviceType.MULTI_SWITCH) listOf(
                            SubSwitch("1", "Switch 1", DeviceStatus.OFF),
                            SubSwitch("2", "Switch 2", DeviceStatus.OFF)
                        ) else emptyList(),
                        maxOnDurationSeconds = if (type == DeviceType.SCHEDULED_APPLIANCE) 600 else null
                    )
                )
                showAddDeviceDialog = null
            }
        )
    }
}

@Composable
private fun DeviceGridTile(device: Device) {
    val color = device.statusColor()
    Box(
        modifier = Modifier
            .size(24.dp) // Tuned tap target/visual size
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceDialog(onDismiss: () -> Unit, onConfirm: (String, DeviceType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.OUTLET) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Device Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DeviceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace('_', ' ')) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedType) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LazyColumnDeviceList(
    devices: List<Device>,
    onToggle: (Device) -> Unit,
    onSelect: (Device) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceRow(device, onToggle = { onToggle(device) }, onClick = { onSelect(device) })
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    onToggle: () -> Unit,
    onClick: () -> Unit
){

    ElevatedCard(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = MaterialTheme.shapes.large,

        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )

    ){

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically

        ){


            Box(

                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        device.statusColor()
                    )

            )


            Spacer(
                modifier = Modifier.width(16.dp)
            )


            Column(

                modifier = Modifier.weight(1f)

            ){

                Text(

                    text = device.name,

                    style = MaterialTheme.typography.titleMedium

                )


                Text(

                    text =
                        "${device.type.name.replace("_"," ")} • ${device.status.name}",

                    style = MaterialTheme.typography.bodyMedium

                )

            }



            if(device.type != DeviceType.CAMERA){

                Switch(

                    checked = device.status == DeviceStatus.ON,

                    onCheckedChange = {

                        onToggle()

                    }

                )

            }


        }


    }

}

private fun Device.statusColor(): Color = when (status) {
    DeviceStatus.ON -> StatusOn
    DeviceStatus.OFF -> StatusOff
    DeviceStatus.ERROR -> StatusError
    DeviceStatus.DISCONNECTED -> StatusDisconnected
}