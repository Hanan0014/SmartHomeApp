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
            onConfirm = { name ->
                viewModel.addFloor(
                    Floor(id = UUID.randomUUID().toString(), name = name, order = floors.size)
                )
                showAddDialog = false
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
