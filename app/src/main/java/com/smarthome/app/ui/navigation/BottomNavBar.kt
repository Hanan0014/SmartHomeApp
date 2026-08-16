package com.smarthome.app.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState


@Composable
fun BottomNavBar(
    navController: NavHostController
){

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.FloorPlan,
        BottomNavItem.Reports,
    )


    NavigationBar {


        val currentRoute =
            navController
                .currentBackStackEntryAsState()
                .value
                ?.destination
                ?.route


        items.forEach { item ->


            NavigationBarItem(

                selected = currentRoute == item.route,


                onClick = {

                    navController.navigate(item.route){

                        popUpTo("dashboard"){
                            saveState = true
                        }

                        launchSingleTop = true

                        restoreState = true

                    }

                },


                icon = {

                    Icon(
                        item.icon,
                        contentDescription = item.label
                    )

                },


                label = {

                    Text(item.label)

                }

            )

        }


    }

}