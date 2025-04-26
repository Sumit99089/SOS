package com.example.sos.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sos.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var pendingSos by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted && pendingSos) {
            getLocationAndSend(context, vm)
            pendingSos = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (locationPermissions.allPermissionsGranted) {
                    getLocationAndSend(context, vm)
                } else {
                    pendingSos = true
                    locationPermissions.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Warning, contentDescription = "SOS")
            Spacer(Modifier.width(8.dp))
            Text("SOS")
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.logout { navController.navigate("login") } }
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}


private fun getLocationAndSend(context: Context, vm: AuthViewModel) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Check for location permissions
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineLocationGranted && !coarseLocationGranted) {
        Log.e("getLocationAndSend", "Location permission not granted")
        return
    }

    // Create a location request
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000 // 1 second
    ).apply {
        setMaxUpdates(1)
    }.build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locations = locationResult.locations
            if (locations.isNotEmpty()) {
                val location = locations[0]
                Log.d("getLocationAndSend", "Location from updates: $location")
                vm.sendSos(location.latitude, location.longitude)
            } else {
                Log.e("getLocationAndSend", "No location found in callback")
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    try {
        // Try to get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("getLocationAndSend", "Last known location: $location")
                vm.sendSos(it.latitude, it.longitude)
            } ?: run {
                // If last location is null, request location update
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            Log.e("getLocationAndSend", "Failed to get last location", it)

            // Fallback to location update request
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    } catch (e: SecurityException) {
        Log.e("getLocationAndSend", "SecurityException: location permission missing", e)
    } catch (e: Exception) {
        Log.e("getLocationAndSend", "Unexpected error getting location", e)
    }
}
