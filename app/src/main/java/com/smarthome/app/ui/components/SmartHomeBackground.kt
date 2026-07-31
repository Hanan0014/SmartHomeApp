package com.smarthome.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smarthome.app.ui.theme.BackgroundDark


@Composable
fun SmartHomeBackground(
    content: @Composable () -> Unit
){

    Box(

        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)

    ){

        content()

    }

}