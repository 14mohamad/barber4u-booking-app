package com.example.barber4u.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barber4u.ui.screens.*

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen { route -> nav.navigate(route) }
        }

        composable("customer") { CustomerScreen() }
        composable("barber") { BarberScreen() }
        composable("admin") { AdminScreen() }
    }
}
