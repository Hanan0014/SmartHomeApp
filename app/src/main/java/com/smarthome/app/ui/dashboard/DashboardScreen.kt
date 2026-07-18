package com.smarthome.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.data.model.Floor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onFloorSelected: (Floor) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val floors by viewModel.floors.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart Home") }) },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(floors, key = { it.id }) { floor ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onFloorSelected(floor) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(floor.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${floor.gridCols} x ${floor.gridRows} grid",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
}
