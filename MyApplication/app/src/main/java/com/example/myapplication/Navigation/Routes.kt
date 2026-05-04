package com.example.myapplication.Navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.Feature.home.HomeBottomBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.example.myapplication.Feature.home.HomeScreen
import com.example.myapplication.Feature.notification.NotificationScreen
import com.example.myapplication.MQTT.MQTTHandler

@Composable
fun MainScreen(mqttHandler: MQTTHandler) {
    val navController = rememberNavController()
    var selected by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            HomeBottomBar(
                selected = selected,
                onSelect = { index ->
                    selected = index
                    when (index) {
                        0 -> navController.navigate("home")
                        1 -> navController.navigate("notification")
                        2 -> navController.navigate("support")
                        3 -> navController.navigate("setting")
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(mqttHandler) }
            composable("notification"){ NotificationScreen() }
            composable("support") {}
            composable("setting"){}
        }
    }
}


