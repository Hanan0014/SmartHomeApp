package com.smarthome.app.ui.device


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.smarthome.app.data.model.Device


@Composable
fun DeviceMetaChips(device: Device) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        MetaChip(
            text = "Grid (${device.gridX},${device.gridY})"
        )

        MetaChip(
            text = "📍 "
        )



        MetaChip(
            text = "⚡ W"
        )


    }

}

@Composable
fun MetaChip(text:String){

    Text(

        text = text,

        color = Color(0xFF94A3B8),

        fontSize = 10.sp,

        modifier = Modifier
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(
                Color(0xFF0F172A)
            )
            .border(
                1.dp,
                Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )

    )

}