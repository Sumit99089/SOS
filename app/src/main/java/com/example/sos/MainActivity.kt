package com.example.sos

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sos.service.SOSService
import com.example.sos.ui.screens.HomeScreen
import com.example.sos.ui.screens.RegistrationScreen
import com.example.sos.ui.screens.SplashScreen
import com.example.sos.ui.theme.SOSTheme
import com.example.sos.util.PreferencesManager


class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)

        // Request permissions
        requestPermissions()

        setContent {
            SOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(preferencesManager)
                }
            }
        }

        // Start the SOS foreground service
        Intent(this, SOSService::class.java).also { intent ->
            startForegroundService(intent)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                100
            )
        }
    }
}

@Composable
fun AppNavigation(preferencesManager: PreferencesManager) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                navigateToRegistration = {
                    navController.navigate("registration") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                preferencesManager = preferencesManager
            )
        }

        composable("registration") {
            RegistrationScreen(
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("registration") { inclusive = true }
                    }
                },
                preferencesManager = preferencesManager
            )
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                preferencesManager = preferencesManager
            )
        }
    }
}

