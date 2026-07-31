package com.smarthome.app.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.smarthome.app.ui.theme.*

@Composable
fun DashboardHeader(unreadCount: Int, onNotificationClick: () -> Unit){

     Column(

        modifier = Modifier
                  .fillMaxWidth()
                  .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 16.dp
                  )

    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ){

            Column {

                Text(
                    text = "GOOD AFTERNOON",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Hi, Homeowner 👋",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

            }

            Box{

                IconButton(onClick = onNotificationClick){

                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextSecondary
                    )

                }

                if (unreadCount > 0) {

                    Surface(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd),
                        shape = MaterialTheme.shapes.small,
                        color = ErrorRed
                    ){

                        Box(contentAlignment = Alignment.Center) {

                            Text(
                                text = unreadCount.toString(),
                                color = TextPrimary,
                                fontSize = 10.sp
                            )

                        }

                    }

                }

            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            Surface(
                color = SurfaceDark,
                shape = MaterialTheme.shapes.medium
            ) {

                Text(
                    text = "10:32 PM · Local",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = TextSecondary,
                    fontSize = 12.sp
                )

            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Surface(
                color = SuccessGreen.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium
            ){

                Text(
                    text = "🟢 SYNCED",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = SuccessGreen,
                    fontSize = 12.sp
                )

            }

        }

    }

}