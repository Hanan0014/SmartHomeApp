package com.smarthome.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Home


import com.smarthome.app.ui.theme.PrimaryBlue
import com.smarthome.app.data.model.Floor
import com.smarthome.app.ui.device.getDummyFloors
import com.smarthome.app.ui.theme.BackgroundLight
import com.smarthome.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onFloorSelected: (Floor) -> Unit, viewModel: DashboardViewModel = viewModel()) {

    val floors = remember {

        getDummyFloors()

    }


    var showAddDialog by remember { mutableStateOf(false) }
    var floorPendingRename by remember { mutableStateOf<Floor?>(null) }
    var floorPendingDelete by remember { mutableStateOf<Floor?>(null) }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Smart Home",
                        color = Color.White
                    )

                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )

            )

        },


        floatingActionButton = {

            FloatingActionButton(onClick = { showAddDialog = true }) {

                Icon(Icons.Default.Add, contentDescription = "Add floor")

            }

        }

    ) { padding ->


        if (floors.isEmpty()) {

            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {

                Text("No floors yet. Tap + to add your first floor plan.")

            }

        } else {

            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)

            ) {
<<<<<<< HEAD

                Text(
                    text = "Welcome Home",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = "Select a floor to manage your smart devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(16.dp),

                    verticalArrangement = Arrangement.spacedBy(16.dp)

                ) {

                    items(floors, key = { it.id }) { floor ->

                        Card(

                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFloorSelected(floor)
                                },

                            shape = MaterialTheme.shapes.large,

                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )

                        ) {

                            Column(

                                modifier = Modifier.padding(20.dp),

                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {


                                /*Image(

                                    painter = painterResource(

                                        id =
                                            if(floor.planImageName == "floor_plan_1")
                                                R.drawable.floor_plan_1
                                            else
                                                R.drawable.floor_plan_2

                                    ),

                                    contentDescription = floor.name,

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)

                                )*/


                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )


                                Row(

                                    verticalAlignment = Alignment.CenterVertically

                                ){

                                    Icon(

                                        imageVector = Icons.Default.Home,

                                        contentDescription = null,

                                        tint = PrimaryBlue

                                    )


                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )


                                    Text(

                                        text = floor.name,

                                        style = MaterialTheme.typography.titleLarge

                                    )

                                }



                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )


                                Text(

                                    text = "Floor Plan",

                                    style = MaterialTheme.typography.bodyMedium

                                )


                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )


                                Text(

                                    text = "${floor.gridCols} × ${floor.gridRows} Grid",

                                    style = MaterialTheme.typography.bodyMedium

                                )


                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )


                                Text(

                                    text = "Devices : 5",

                                    style = MaterialTheme.typography.bodyMedium

                                )

                            }

                        }

                    }

=======
                items(floors, key = { it.id }) { floor ->
                    FloorCard(
                        floor = floor,
                        onClick = { onFloorSelected(floor) },
                        onRenameClick = { floorPendingRename = floor },
                        onDeleteClick = { floorPendingDelete = floor }
                    )
>>>>>>> 1a0fb62b9d0fdb8cd3b99f7e879d29cfd8c93baa
                }

            }
        }
    }

    if (showAddDialog) {
        AddFloorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, planImage ->
                viewModel.addFloor(
                    Floor(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        planImageName = planImage,
                        order = floors.size
                    )
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
private fun FloorCard(
    floor: Floor,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(floor.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${floor.gridCols} x ${floor.gridRows} grid",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Always-visible icon button -- no long-press or hidden gesture required.
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Floor options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRenameClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFloorDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("floor_plan_1") }
    var expanded by remember { mutableStateOf(false) }
    val plans = listOf("floor_plan_1", "floor_plan_2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Floor Plan") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Floor name, e.g. Ground Floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPlan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Background Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text(plan) },
                                onClick = {
                                    selectedPlan = plan
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedPlan) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
<<<<<<< HEAD
=======
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
>>>>>>> 1a0fb62b9d0fdb8cd3b99f7e879d29cfd8c93baa
}