package com.smarthome.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue


@Composable
fun SmartHomeApp(){

    val navController = rememberNavController()


    val navBackStackEntry by navController.currentBackStackEntryAsState()


    val currentRoute = navBackStackEntry?.destination?.route


    Scaffold(

        bottomBar = {

            if(
                currentRoute != "login" &&
                currentRoute != "device_detail"
            ){

                BottomNavBar(
                    navController = navController
                )

            }

        }

    ){ paddingValues ->


        SmartHomeNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )

    }

}