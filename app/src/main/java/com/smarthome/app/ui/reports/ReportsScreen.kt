package com.smarthome.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.data.repository.DeviceWithFloor
import com.smarthome.app.ui.components.SmartHomeBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_FORMAT = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    val devices by viewModel.displayedDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    SmartHomeBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
                    title = { Text("Usage Reports") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Sort toggle — most used vs. most recently toggled.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortMode == ReportSort.MOST_USED,
                        onClick = { viewModel.setSortMode(ReportSort.MOST_USED) },
                        label = { Text("Most Used") }
                    )
                    FilterChip(
                        selected = sortMode == ReportSort.RECENTLY_TOGGLED,
                        onClick = { viewModel.setSortMode(ReportSort.RECENTLY_TOGGLED) },
                        label = { Text("Recently Toggled") }
                    )
                }

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    devices.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No devices yet. Add a floor and some devices to see usage data here.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(devices, key = { it.device.id }) { entry ->
                                DeviceUsageRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceUsageRow(entry: DeviceWithFloor) {
    val device = entry.device
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(device.status.name, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${entry.floorName} • ${device.type.name.replace("_", " ")}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Total ON time: ${formatDuration(device.totalOnTimeSeconds)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Last toggled: ${
                    device.lastToggledAtEpochMs?.let { DATE_FORMAT.format(Date(it)) } ?: "Never"
                }",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}