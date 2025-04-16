package com.example.sos.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

private fun getLocationAndSend(context: Context, vm: AuthViewModel) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            location?.let {
                vm.sendSos(it.latitude, it.longitude)
            } ?: run {
                // Handle case where location is null
            }
        }
        .addOnFailureListener { exception ->
            when (exception) {
                is SecurityException -> {
                    // Handle permission-related errors
                }
                else -> {
                    // Handle other errors
                }
            }
        }
}