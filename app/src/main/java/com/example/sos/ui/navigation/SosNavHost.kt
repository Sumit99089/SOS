package com.example.sos.ui.navigation


import RegistrationScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sos.ui.screens.*

@Composable
fun SosNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "register") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegistrationScreen(navController) }
        composable("home") { HomeScreen(navController) }
    }
}