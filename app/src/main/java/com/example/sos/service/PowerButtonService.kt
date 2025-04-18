package com.example.sos.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.example.sos.MainActivity
import com.example.sos.R
import com.example.sos.repository.UserRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerButtonService : Service() {
    private var count = 0
    private var lastTime = 0L
    private val userRepo by lazy { UserRepository(applicationContext) }
    private val CHANNEL_ID = "SosServiceChannel"
    private val NOTIFICATION_ID = 1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val now = System.currentTimeMillis()
            if (now - lastTime <= 5000) count++ else count = 1
            lastTime = now
            Log.d("PowerButtonService", "Power button pressed, count: $count")
            if (count == 5) {
                Log.d("PowerButtonService", "SOS trigger detected!")
                if (hasLocationPermissions()) {
                    triggerSos()
                } else {
                    Log.e("PowerButtonService", "Cannot send SOS - missing location permissions")
                }
                count = 0
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PowerButtonService", "Service created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PowerButtonService", "Service started")
        // Return START_STICKY to indicate that the system should recreate the service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("PowerButtonService", "Service destroyed")
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Error unregistering receiver", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermissions(): Boolean {
        // Check if we have the necessary permissions
        return true // Replace with actual permission check
    }

    private fun triggerSos() {
        Log.d("PowerButtonService", "Attempting to get location and send SOS")
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    Log.d("PowerButtonService", "Location obtained: ${loc.latitude}, ${loc.longitude}")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            userRepo.sendSos(loc.latitude, loc.longitude)
                            Log.d("PowerButtonService", "SOS sent successfully")
                        } catch (e: Exception) {
                            Log.e("PowerButtonService", "Error sending SOS", e)
                        }
                    }
                } else {
                    Log.e("PowerButtonService", "Location is null")
                }
            }.addOnFailureListener { e ->
                Log.e("PowerButtonService", "Failed to get location", e)
            }
        } catch (e: SecurityException) {
            Log.e("PowerButtonService", "Security exception when accessing location", e)
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Unexpected error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SOS Service"
            val descriptionText = "Keeps the SOS service running"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS Service Running")
            .setContentText("Press power button 5 times quickly to send SOS")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with appropriate icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}