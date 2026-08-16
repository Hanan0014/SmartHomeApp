package com.smarthome.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.ui.components.SmartHomeBackground
import com.smarthome.app.data.model.Floor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onFloorSelected: (Floor) -> Unit,
    onReportsClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val floors by viewModel.floors.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var floorPendingRename by remember { mutableStateOf<Floor?>(null) }
    var floorPendingDelete by remember { mutableStateOf<Floor?>(null) }

    SmartHomeBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
                    title = { Text("Smart Home") },
                    actions = {
                        IconButton(onClick = onReportsClick) {
                            Icon(Icons.Default.Assessment, contentDescription = "Usage Reports")
                        }
                    }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(floors, key = { it.id }) { floor ->
                        FloorCard(
                            floor = floor,
                            onClick = { onFloorSelected(floor) },
                            onRenameClick = { floorPendingRename = floor },
                            onDeleteClick = { floorPendingDelete = floor }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFloorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, gridCols, gridRows ->
                viewModel.addFloor(
                    Floor(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        order = floors.size,
                        gridCols = gridCols,
                        gridRows = gridRows
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111827)
        ),

        border = BorderStroke(
            1.dp,
            Color(0xFF334155)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(floor.name, color = Color.White, fontSize = 17.sp, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${floor.gridCols} x ${floor.gridRows} grid",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
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

private const val MIN_GRID_SIZE = 3
private const val MAX_GRID_SIZE = 15
private const val DEFAULT_GRID_COLS = 8
private const val DEFAULT_GRID_ROWS = 6

@Composable
private fun AddFloorDialog(onDismiss: () -> Unit, onConfirm: (name: String, gridCols: Int, gridRows: Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var colsText by remember { mutableStateOf(DEFAULT_GRID_COLS.toString()) }
    var rowsText by remember { mutableStateOf(DEFAULT_GRID_ROWS.toString()) }

    val nameError = name.isNotEmpty() && name.isBlank()
    val cols = colsText.toIntOrNull()
    val rows = rowsText.toIntOrNull()
    val colsError = cols == null || cols !in MIN_GRID_SIZE..MAX_GRID_SIZE
    val rowsError = rows == null || rows !in MIN_GRID_SIZE..MAX_GRID_SIZE
    val canSubmit = name.isNotBlank() && !colsError && !rowsError

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
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name can't be blank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Grid size (customize how many cells your floor plan has)",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = colsText,
                        onValueChange = { colsText = it },
                        label = { Text("Columns") },
                        singleLine = true,
                        isError = colsError,
                        supportingText = { if (colsError) Text("$MIN_GRID_SIZE-$MAX_GRID_SIZE") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rowsText,
                        onValueChange = { rowsText = it },
                        label = { Text("Rows") },
                        singleLine = true,
                        isError = rowsError,
                        supportingText = { if (rowsError) Text("$MIN_GRID_SIZE-$MAX_GRID_SIZE") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, cols ?: DEFAULT_GRID_COLS, rows ?: DEFAULT_GRID_ROWS) },
                enabled = canSubmit
            ) { Text("Add") }
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