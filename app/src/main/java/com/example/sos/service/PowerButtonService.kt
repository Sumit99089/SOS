package com.example.sos.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
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
    private val SOS_NOTIFICATION_ID = 911
    private val COUNTDOWN_CHANNEL_ID = "SosCountdownChannel"

    // Track if SOS countdown is in progress
    private var sosCountdownTimer: CountDownTimer? = null
    private var sosLocation: Location? = null

    companion object {
        const val ACTION_CANCEL_SOS = "com.example.sos.action.CANCEL_SOS"
        const val COUNTDOWN_DURATION = 20000L // 10 seconds
        const val COUNTDOWN_INTERVAL = 1000L // 1 second for updates
    }

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
                        prepareAndStartSosCountdown()
                    } else {
                        Log.e("PowerButtonService", "Cannot send SOS - missing location permissions")
                    }
                    count = 0
                }
            }
            // Check if it's the cancel SOS action
            else if (intent?.action == ACTION_CANCEL_SOS) {
                Log.d("PowerButtonService", "SOS cancelled by user")
                cancelSosCountdown()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Log.d("PowerButtonService", "Service created")
        createNotificationChannel()
        createCountdownNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Register for both screen on and off events to detect power button presses
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(ACTION_CANCEL_SOS)
        }
        registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PowerButtonService", "Service started")
        if (intent?.action == ACTION_CANCEL_SOS) {
            cancelSosCountdown()
        }
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

        // Cancel countdown timer if active
        sosCountdownTimer?.cancel()

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

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent)

        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermissions(): Boolean {
        // Check if we have the necessary permissions
        return true // Replace with actual permission check
    }

    private fun prepareAndStartSosCountdown() {
        Log.d("PowerButtonService", "Preparing SOS countdown")
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    sosLocation = loc
                    Log.d("PowerButtonService", "Location obtained: ${loc.latitude}, ${loc.longitude}")
                    startSosCountdown()
                } else {
                    Log.e("PowerButtonService", "Location is null")
                    // Handle case where location is null - maybe start countdown anyway
                    startSosCountdown()
                }
            }.addOnFailureListener { e ->
                Log.e("PowerButtonService", "Failed to get location", e)
                // Start countdown even if we couldn't get location
                startSosCountdown()
            }
        } catch (e: SecurityException) {
            Log.e("PowerButtonService", "Security exception when accessing location", e)
            // Start countdown even with error
            startSosCountdown()
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Unexpected error", e)
            startSosCountdown()
        }
    }

    private fun startSosCountdown() {
        Log.d("PowerButtonService", "Starting SOS countdown timer")

        // Cancel any existing countdown
        cancelSosCountdown()

        // Create the notification with a progress bar at 100% (10 seconds)
        showCountdownNotification(10)

        // Create and start a new countdown timer
        sosCountdownTimer = object : CountDownTimer(COUNTDOWN_DURATION, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                Log.d("PowerButtonService", "SOS countdown: $secondsRemaining seconds remaining")
                // Update the existing notification to show progress
                showCountdownNotification(secondsRemaining)
            }

            override fun onFinish() {
                Log.d("PowerButtonService", "SOS countdown finished, sending SOS")
                // Timer finished without cancellation, send the SOS
                sendSosToServer()
            }
        }.start()
    }

    private fun cancelSosCountdown() {
        sosCountdownTimer?.cancel()
        sosCountdownTimer = null

        // Remove the countdown notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SOS_NOTIFICATION_ID)

        // Let the user know SOS was cancelled
        showSosCancelledNotification()
    }

    private fun sendSosToServer() {
        Log.d("PowerButtonService", "Sending SOS to server")
        sosLocation?.let { loc ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRepo.sendSos(loc.latitude, loc.longitude)
                    Log.d("PowerButtonService", "SOS sent successfully")

                    // Show emergency notification that will work on lock screen
                    showSosTriggeredNotification()

                    // Also try to open app directly if device is not locked
                    val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
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
        } ?: run {
            Log.e("PowerButtonService", "Cannot send SOS - location is null")
        }
    }

    private fun createCountdownNotificationChannel() {
        val name = "SOS Countdown"
        val descriptionText = "Countdown for SOS trigger"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(COUNTDOWN_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(true)
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showCountdownNotification(secondsRemaining: Int) {
        // 1. Intent for WHEN USER TAPS NOTIFICATION - opens app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Intent for CANCEL ACTION - triggers service
        val cancelIntent = Intent(this, PowerButtonService::class.java).apply {
            action = ACTION_CANCEL_SOS
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with corrected intents
        val notification = NotificationCompat.Builder(this, COUNTDOWN_CHANNEL_ID)
            .setContentTitle("SOS Alert Countdown")
            .setContentText("SOS will be sent in $secondsRemaining seconds. Tap to open app.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent) // Use ACTIVITY intent here
            .setProgress(10, secondsRemaining, false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel SOS",
                cancelPendingIntent // Service intent ONLY in action
            )
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SOS_NOTIFICATION_ID, notification)
    }

    private fun showSosCancelledNotification() {
        // Create a notification to let the user know SOS was cancelled
        val notification = NotificationCompat.Builder(this, COUNTDOWN_CHANNEL_ID)
            .setContentTitle("SOS Alert Cancelled")
            .setContentText("Your SOS alert has been cancelled.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SOS_NOTIFICATION_ID + 1, notification)

        // Auto dismiss after 3 seconds
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(3000)
            notificationManager.cancel(SOS_NOTIFICATION_ID + 1)
        }
    }

    // Original showSosTriggeredNotification method
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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setContentTitle("SOS Alert Sent")
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
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SOS_NOTIFICATION_ID, notification)  // Use unique ID for emergency notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SOS Service"
            val descriptionText = "Keeps the SOS service running"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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