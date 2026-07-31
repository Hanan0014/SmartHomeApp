package com.smarthome.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.theme.BackgroundDark
import com.smarthome.app.ui.theme.PrimaryCyan
import com.smarthome.app.ui.components.SmartHomeBackground


@Composable
fun SmartHomeBackground(content: @Composable () -> Unit) {

    Box(

        modifier = Modifier
                  .fillMaxSize()
                  .background(BackgroundDark)

    ) {

        content()

    }

}