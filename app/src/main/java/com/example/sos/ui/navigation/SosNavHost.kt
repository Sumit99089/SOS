package com.example.sos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sos.ui.screens.*

@Composable
fun SosNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("login") { LoginScreen(navController) }
        composable("register_email") { RegisterEmailScreen(navController) }
        composable("register_name") { RegisterNameScreen(navController) }
        composable("register_password") { RegisterPasswordScreen(navController) }
        composable("home") { HomeScreen(navController) }
    }
}