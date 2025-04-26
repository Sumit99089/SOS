package com.example.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.sos.service.PowerButtonService
import com.example.sos.ui.navigation.SosNavHost
import com.example.sos.ui.theme.SOSTheme
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.app.Activity
import android.content.IntentSender
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startForegroundService()
        } else {
            // Handle permission denial
            Log.e("MainActivity", "Location permissions not granted, SOS functionality will be limited")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if app was opened due to SOS trigger
        if (intent?.action == "com.example.sos.ACTION_LOCATION_SETTINGS") {
            // Show location settings dialog
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000L // 10 seconds interval
            ).build()

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

            val client = LocationServices.getSettingsClient(this)
            val task = client.checkLocationSettings(builder.build())

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val activity = this as? Activity
                        activity?.let {
                            exception.startResolutionForResult(it, 1001)
                        } ?: run {
                            Log.e("MainActivity", "Context is not an Activity")
                        }
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e("MainActivity", "Error showing location settings dialog", e)
                    }
                }
            }
        }

        checkAndRequestPermissions()

        setContent {
            SOSTheme {
                SosNavHost()
            }
        }
    }
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            startForegroundService()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun startForegroundService() {
        Log.d("MainActivity", "Starting foreground service")
        val serviceIntent = Intent(application, PowerButtonService::class.java)
        startForegroundService(serviceIntent)
    }
}