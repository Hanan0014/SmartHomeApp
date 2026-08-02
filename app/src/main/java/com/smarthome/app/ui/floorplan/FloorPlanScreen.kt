package com.smarthome.app.ui.floorplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.app.R
import com.smarthome.app.ui.components.SmartHomeBackground
import com.smarthome.app.ui.components.ScrollableScreen
s

private const val GRID_ROWS = 6
private const val GRID_COLS = 8

data class DemoDevice(
    val id: String,
    val name: String,
    val room: String,
    val type: String,
    val status: String,
    val icon: String,
    val gridX: Int,
    val gridY: Int,
    val power: Int? = null
)

data class DemoFloor(
    val id: String,
    val name: String,
    val level: Int,
    val image: Int,
    val devices: List<DemoDevice>
)

private val groundFloor = DemoFloor(
    id = "1",
    name = "Ground Floor",
    level = 1,
    image = R.drawable.iron,
    devices = listOf(
        DemoDevice(
            "1",
            "Living Outlet",
            "Living Room",
            "OUTLET",
            "ON",
            "🔌",
            1,
            1,
            120
        ),
        DemoDevice(
            "2",
            "Iron",
            "Laundry",
            "SCHEDULED",
            "OFF",
            "🔥",
            3,
            4
        ),
        DemoDevice(
            "3",
            "Front Camera",
            "Entrance",
            "CAMERA",
            "ON",
            "📷",
            6,
            2
        ),
        DemoDevice(
            "4",
            "Kitchen Light",
            "Kitchen",
            "LIGHT",
            "ERROR",
            "💡",
            5,
            5
        ),
        DemoDevice(
            "5",
            "Bedroom Switch",
            "Bedroom",
            "MULTI",
            "DISCONNECTED",
            "🎛️",
            7,
            1
        )
    )
)

private val firstFloor = DemoFloor(
    id = "2",
    name = "First Floor",
    level = 2,
    image = R.drawable.iron,
    devices = listOf(
        DemoDevice(
            "6",
            "Balcony Light",
            "Balcony",
            "LIGHT",
            "ON",
            "💡",
            2,
            2
        ),
        DemoDevice(
            "7",
            "Bedroom Camera",
            "Bedroom",
            "CAMERA",
            "OFF",
            "📷",
            5,
            4
        )
    )
)

private val floors = listOf(
    groundFloor,
    firstFloor
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(onBack: () -> Unit = {}, onDeviceClick: (String) -> Unit = {}){

    var activeFloor by remember {
        mutableStateOf(floors.first())
    }

    var selectedDevice by remember {
        mutableStateOf<DemoDevice?>(null)
    }

    SmartHomeBackground {

        Scaffold(

            containerColor = Color.Transparent,

            topBar = {

                TopAppBar(

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),

                    title = {

                        Column {

                            Text(
                                "Floor Plan",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )

                        }

                    },

                    navigationIcon = {

                        IconButton(onClick = onBack) {

                            Icon(
                                Icons.Default.ArrowBack,
                                null
                            )

                        }

                    },

                    actions = {

                        Button(

                            onClick = {
                                // open add floor dialog here
                            },

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x1422D3EE),
                                contentColor = Color(0xFF22D3EE)
                            ),

                            border = BorderStroke(
                                1.dp,
                                Color(0x3322D3EE)
                            ),

                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )

                        ) {

                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(6.dp)
                            )

                            Text(
                                text = "Add Floor",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )

                        }

                    }

                )

            }

        ) { padding ->

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),

                verticalArrangement = Arrangement.spacedBy(16.dp)

            ) {

                item {

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(floors) { floor ->

                            val selected = floor.id == activeFloor.id

                            Button(

                                onClick = {

                                    activeFloor = floor
                                    selectedDevice = null

                                },

                                shape = RoundedCornerShape(8.dp),

                                colors = ButtonDefaults.buttonColors(

                                    containerColor = if (selected)
                                                        Color(0x2622D3EE)
                                                     else
                                                        Color(0xFF0F172A),

                                    contentColor = if (selected)
                                                      Color(0xFF22D3EE)
                                                   else
                                                      Color(0xFF94A3B8)

                                ),

                                border = BorderStroke(

                                    1.dp,

                                    if (selected)
                                        Color(0x4D22D3EE)
                                    else
                                        Color(0xFF1E293B)

                                ),

                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )

                            ) {

                                Text(

                                    text = "L${floor.level} · ${floor.name.split(" ")[0]}",

                                    fontSize = 16.sp

                                )

                            }

                        }

                    }

                }

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                    ) {

                        Image(
                            painter = painterResource(activeFloor.image),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.25f
                        )

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            repeat(GRID_ROWS) { row ->

                                Row(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    repeat(GRID_COLS) { col ->

                                        val device = activeFloor.devices.find {
                                            it.gridX == col && it.gridY == row
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(0.5.dp, Color(0x3322D3EE))
                                                .clickable {
                                                    selectedDevice = device
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {

                                            if (device != null) {

                                                val color =
                                                    when (device.status) {

                                                        "ON" -> Color(0xFF22C55E)

                                                        "OFF" -> Color.Gray

                                                        "ERROR" -> Color.Red

                                                        else -> Color.Yellow

                                                    }

                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(
                                                            color.copy(alpha = 0.15f),
                                                            CircleShape
                                                        )
                                                        .border(
                                                            2.dp,
                                                            color,
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {

                                                    Text(
                                                        device.icon,
                                                        fontSize = 16.sp
                                                    )

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xD9090D18)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                LegendItem("ON", Color(0xFF22C55E))
                                LegendItem("OFF", Color.Gray)
                                LegendItem("ERROR", Color.Red)
                                LegendItem("N/C", Color(0xFFEAB308))

                            }
                        }

                    }

                }

                item {

                    Text(
                        text = "Devices on ${activeFloor.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                }

                items(activeFloor.devices) { device ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeviceClick(device.id)
                            },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = device.icon,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = device.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "${device.room} · ${device.type}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                            }

                            if (device.power != null && device.status == "ON") {
                                Text(
                                    text = "${device.power}W",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            val statusColor = when (device.status) {
                                "ON" -> Color(0xFF22C55E)
                                "OFF" -> Color.Gray
                                "ERROR" -> Color(0xFFEF4444)
                                else -> Color(0xFFEAB308)
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

            }


        }

    }


}

@Composable
fun LegendItem(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )

        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}