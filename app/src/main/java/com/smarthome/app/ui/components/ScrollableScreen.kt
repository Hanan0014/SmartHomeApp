package com.smarthome.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.components.ScrollableScreen


@Composable
fun ScrollableScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
){

    Column(

        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(bottom = 24.dp),

        content = content

    )

}