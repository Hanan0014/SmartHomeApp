package com.smarthome.app.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.app.data.model.Floor
import com.smarthome.app.ui.theme.*
import androidx.compose.foundation.clickable


@Composable
fun FloorCard(floor: Floor, deviceCount: Int, activeDevices: Int, onClick: () -> Unit) {

    Surface(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = MaterialTheme.shapes.large,

        color = SurfaceDark

    ){

        Row(

            modifier = Modifier
                      .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically

        ){

            Surface(

                modifier = Modifier.size(45.dp),

                shape = MaterialTheme.shapes.medium,

                color = SurfaceDarkSecondary

            ){

                Box(

                    contentAlignment = Alignment.Center

                ){

                    Text(

                        text = "L${floor.order + 1}",

                        color = PrimaryCyan,

                        fontSize = 14.sp

                    )

                }

            }


            Spacer(
                modifier = Modifier.width(16.dp)
            )



            Column(
                modifier = Modifier.weight(1f)
            ){

                Text(

                    text = floor.name,

                    color = TextPrimary,

                    fontSize = 16.sp

                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )


                Text(

                    text = "$deviceCount devices · $activeDevices active",

                    color = TextSecondary,

                    fontSize = 12.sp

                )

            }

            Text(

                text = "›",

                color = TextSecondary,

                fontSize = 30.sp

            )

        }

    }

}