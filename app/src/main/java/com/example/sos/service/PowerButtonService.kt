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
            // Check if it's the power button press event
            if (intent?.action == Intent.ACTION_SCREEN_ON || intent?.action == Intent.ACTION_SCREEN_OFF) {
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
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PowerButtonService", "Service created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Register for both screen on and off events to detect power button presses
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(receiver, intentFilter)
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

        // Request restart if service is killed
        val restartServiceIntent = Intent(applicationContext, PowerButtonService::class.java)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("PowerButtonService", "Task removed")
        // Request restart if the app is removed from recents
        val restartServiceIntent = Intent(applicationContext, PowerButtonService::class.java)
        restartServiceIntent.setPackage(packageName)
        val pendingIntent = PendingIntent.getService(
            this, 1, restartServiceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent)

        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermissions(): Boolean {
        // Check if we have the necessary permissions
        return true // Replace with actual permission check
    }

    // Add this new method to PowerButtonService.kt
    private fun showSosTriggeredNotification() {
        // Create an emergency notification channel with higher priority
        val emergencyChannelId = "SosEmergencyChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SOS Emergency Alerts"
            val desc = "High priority alerts when SOS is triggered"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(emergencyChannelId, name, importance).apply {
                description = desc
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "com.example.sos.SOS_TRIGGERED"
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the emergency notification
        val notification = NotificationCompat.Builder(this, emergencyChannelId)
            .setContentTitle("SOS Alert Triggered")
            .setContentText("Emergency services have been notified. Tap to open app.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)  // This helps appear on lock screen
            .setAutoCancel(true)
            .setOngoing(true)  // Makes notification persistent
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open App",
                contentPendingIntent
            )
            .build()

        // Show the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(911, notification)  // Use unique ID for emergency notification
    }

    // Modify the triggerSos method to use the new notification approach
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

                            // Show emergency notification that will work on lock screen
                            showSosTriggeredNotification()

                            // Also try to open app directly if device is not locked
                            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                            if (!keyguardManager.isKeyguardLocked) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        action = "com.example.sos.SOS_TRIGGERED"
                                    }
                                    startActivity(launchIntent)
                                }
                            }
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