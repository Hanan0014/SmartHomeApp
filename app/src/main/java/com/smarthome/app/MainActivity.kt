package com.smarthome.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smarthome.app.ui.navigation.SmartHomeNavGraph
import com.smarthome.app.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartHomeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartHomeNavGraph()
                }
            }
        }
    }
}
