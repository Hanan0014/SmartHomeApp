
package com.smarthome.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue




@Composable
fun LoginScreen(onLogin: () -> Unit) {

    var userId by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF090D18))) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val spacing = 28.dp.toPx()

            var x = 0f

            while (x < size.width) {

                drawLine(
                    color = Color(0x3322D3EE),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height)
                )

                x += spacing

            }

            var y = 0f

            while (y < size.height) {

                drawLine(
                    color = Color(0x3322D3EE),
                    start = Offset(0f, y),
                    end = Offset(size.width, y)
                )

                y += spacing
            }

        }

        
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3322D3EE),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(120.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF164E63),
                                Color(0xFF0E7490)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Smart Home Logo",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(36.dp)
                )

            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SmartHome",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "CONTROL SYSTEM v2.4",
                color = Color(0xFF22D3EE),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Monitor and control every device\nin your home from one place.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(modifier = Modifier.fillMaxWidth()){

                Text(
                    text = "USER ID",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                OutlinedTextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF1F2D45),

                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PASSPHRASE",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF1F2D45),

                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0891B2)
                    )
                ) {

                    Text(
                        text = "LOGIN",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                }

            }

        }










    }

}