package com.example.sos.service

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.IBinder
import androidx.annotation.RequiresPermission
import com.example.sos.repository.UserRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerButtonService: Service() {
    private var count = 0
    private var lastTime = 0L
    private lateinit var userRepo: UserRepository
    private val receiver = object: BroadcastReceiver() {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onReceive(context: Context?, intent: Intent?) {
            val now = System.currentTimeMillis()
            if (now - lastTime <= 5000) count++ else count = 1
            lastTime = now
            if (count >= 5) triggerSos()
        }
    }

    override fun onCreate() {
        super.onCreate()
        userRepo = UserRepository(applicationContext)
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun triggerSos() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    userRepo.sendSos(loc.latitude, loc.longitude)
                }
            }
        }
    }
}