package com.example.sos.service

import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.sos.MainActivity
import com.example.sos.R
import com.example.sos.repository.UserRepository
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PowerButtonService : Service() {
    private var count = 0
    private var lastTime = 0L
    private val userRepo by lazy { UserRepository(applicationContext) }
    private val CHANNEL_ID = "SosServiceChannel"
    private val NOTIFICATION_ID = 1
    private val SOS_NOTIFICATION_ID = 911
    private val COUNTDOWN_CHANNEL_ID = "SosCountdownChannel"

    // Location tracking
    private var sosCountdownTimer: CountDownTimer? = null
    private var sosLocation: Location? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        const val ACTION_CANCEL_SOS = "com.example.sos.action.CANCEL_SOS"
        const val COUNTDOWN_DURATION = 20000L // 20 seconds
        const val COUNTDOWN_INTERVAL = 1000L // 1 second updates
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("PowerButtonService", "Received action: $action")

            when (action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF -> {
                    Log.d("PowerButtonService", "Screen state changed")
                    handlePowerButtonPress()
                }
                ACTION_CANCEL_SOS -> {
                    Log.d("PowerButtonService", "Cancel SOS action received")
                    cancelSosCountdown()
                }
            }
        }

        private fun handlePowerButtonPress() {
            val now = System.currentTimeMillis()
            count = if (now - lastTime <= 5000) count + 1 else 1
            lastTime = now

            Log.d("PowerButtonService", "Power button press detected, count: $count")

            if (count == 5) {
                Log.d("PowerButtonService", "SOS trigger detected!")
                // Reset count immediately to prevent multiple triggers
                count = 0

                if (hasLocationPermissions()) {
                    if (isLocationEnabled()) {
                        checkLocationSettings()
                    } else {
                        Log.e("PowerButtonService", "Location providers disabled")
                        startSosCountdown()
                    }
                } else {
                    Log.e("PowerButtonService", "Missing location permissions")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        initializeService()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initializeService() {
        Log.d("PowerButtonService", "Service created")
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createServiceNotification())
        registerPowerButtonReceiver()
    }

    private fun createNotificationChannels() {
        createNotificationChannel(
            CHANNEL_ID,
            "SOS Service",
            "Keeps the SOS service running",
            NotificationManager.IMPORTANCE_LOW
        )

        createNotificationChannel(
            COUNTDOWN_CHANNEL_ID,
            "SOS Countdown",
            "Countdown for SOS trigger",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(true)
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerPowerButtonReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(ACTION_CANCEL_SOS)
        }
        registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_SOS) {
            cancelSosCountdown()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cleanupResources()
        restartServiceIfNeeded()
        super.onDestroy()
    }

    private fun cleanupResources() {
        try {
            unregisterReceiver(receiver)
            locationCallback?.let {
                LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it)
            }
            sosCountdownTimer?.cancel()
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Error during cleanup", e)
        }
    }

    private fun restartServiceIfNeeded() {
        val restartIntent = Intent(applicationContext, PowerButtonService::class.java).apply {
            setPackage(packageName)
        }
        startService(restartIntent)
    }

    /* Location and SOS Functions */
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).apply {
            setWaitForAccurateLocation(true)
            setMinUpdateIntervalMillis(5000)
            setMaxUpdates(1)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())
            .addOnSuccessListener { prepareAndStartSosCountdown() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            action = "com.example.sos.ACTION_LOCATION_SETTINGS"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        Log.e("PowerButtonService", "Error opening settings", e)
                        startSosCountdown()
                    }
                } else {
                    Log.e("PowerButtonService", "Location settings inadequate", exception)
                    startSosCountdown()
                }
            }
    }

    private fun prepareAndStartSosCountdown() {
        // First, your own helper (e.g. prompts if needed)
        Log.d("PowerButtonService", "Preparing to start SOS countdown")
        if (!hasLocationPermissions()) {
            Log.e("PowerButtonService", "Location permissions not granted")
            startSosCountdown()
            return
        }

        // Obtain the FusedLocationProviderClient
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            // Runtime permission check so Lint is satisfied
            val fineOK = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseOK = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fineOK && !coarseOK) {
                Log.e("PowerButtonService", "Location permissions missing at runtime")
                startSosCountdown()
                return
            }

            // Build a single-update, high-accuracy request
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1_000L
            )
                .setMaxUpdates(1)
                .build()

            // Callback to capture the incoming location
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    cleanupLocationUpdates()
                    locationResult.locations.firstOrNull()?.let { loc ->
                        sosLocation = loc
                        Log.d(
                            "PowerButtonService",
                            "Location obtained via callback: ${loc.latitude}, ${loc.longitude}"
                        )
                    }
                    startSosCountdown()
                }
            }

            // Start the request
            @SuppressLint("MissingPermission") // we’ve just checked above
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Backup: pull last known location if callback is delayed
            @SuppressLint("MissingPermission")
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.takeIf { sosLocation == null }?.also { loc ->
                        sosLocation = loc
                        Log.d(
                            "PowerButtonService",
                            "Last location fallback: ${loc.latitude}, ${loc.longitude}"
                        )
                    }
                }

            // Timeout in case no update arrives
            Handler(Looper.getMainLooper()).postDelayed({
                cleanupLocationUpdates()
                if (sosLocation == null) {
                    Log.d("PowerButtonService", "Location timeout")
                }
                startSosCountdown()
            }, 5_000L)

        } catch (e: SecurityException) {
            // In the unlikely event of a permission slip
            Log.e("PowerButtonService", "SecurityException fetching location", e)
            startSosCountdown()
        } catch (e: Exception) {
            // Any other failures
            Log.e("PowerButtonService", "Location error", e)
            startSosCountdown()
        }
    }
    private fun cleanupLocationUpdates() {
        Log.d("PowerButtonService", "Cleaning up location updates")
        locationCallback?.let {
            LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun startSosCountdown() {
//        cancelSosCountdown() // Cancel any existing countdown

        showCountdownNotification(20) // Initial 10-second countdown

        sosCountdownTimer = object : CountDownTimer(COUNTDOWN_DURATION, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                showCountdownNotification(seconds)
            }

            override fun onFinish() {
                sendSosToServer()
            }
        }.start()
    }

    private fun cancelSosCountdown() {
        Log.d("cancelsos", "cancelSosCountdown")
        sosCountdownTimer?.cancel()
        sosCountdownTimer = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(SOS_NOTIFICATION_ID)
        showSosCancelledNotification()
    }

    private fun sendSosToServer() {
        sosLocation?.let { location ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRepo.sendSos(location.latitude, location.longitude)
                    showSosTriggeredNotification()
                    openAppIfUnlocked()
                } catch (e: Exception) {
                    Log.e("PowerButtonService", "Failed to send SOS", e)
                }
            }
        } ?: Log.e("PowerButtonService", "No location available")
    }

    private fun openAppIfUnlocked() {
        if (!(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "com.example.sos.SOS_TRIGGERED"
            })
        }
    }

    /* Notification Helpers */
    private fun createNotificationChannel(
        channelId: String,
        name: String,
        description: String,
        importance: Int
    ): NotificationChannel {
        return NotificationChannel(channelId, name, importance).apply {
            this.description = description
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(this)
        }
    }

    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS Service Running")
            .setContentText("Press power button 5 times quickly to send SOS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCountdownNotification(secondsRemaining: Int) {
        val notification = NotificationCompat.Builder(this, COUNTDOWN_CHANNEL_ID)
            .setContentTitle("SOS Alert Countdown")
            .setContentText("SOS will be sent in $secondsRemaining seconds")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createActivityPendingIntent())
            .setProgress(10, secondsRemaining, false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete, // System cancel icon
                "Cancel SOS",
                createCancelPendingIntent()
            )
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(SOS_NOTIFICATION_ID, notification)
    }

    private fun showSosCancelledNotification() {
        val notification = NotificationCompat.Builder(this, COUNTDOWN_CHANNEL_ID)
            .setContentTitle("SOS Alert Cancelled")
            .setContentText("Your SOS alert has been cancelled.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SOS_NOTIFICATION_ID + 1, notification)

        CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
            notificationManager.cancel(SOS_NOTIFICATION_ID + 1)
        }
    }

    private fun showSosTriggeredNotification() {
        val emergencyChannelId = "SosEmergencyChannel"

        // Create or get existing channel
        val channel = NotificationChannel(
            emergencyChannelId,
            "SOS Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority alerts when SOS is triggered"

            // Sound configuration
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)

            // Other important settings
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Register the channel
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, emergencyChannelId)
            .setContentTitle("SOS Alert Sent!")
            .setContentText("Emergency services notified. Tap to open app.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createActivityPendingIntent())
            .setFullScreenIntent(createActivityPendingIntent(), true)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_view, // System open icon
                "Open App",
                createActivityPendingIntent()
            )
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(SOS_NOTIFICATION_ID, notification)
    }


    private fun createActivityPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "com.example.sos.SOS_TRIGGERED"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCancelPendingIntent(): PendingIntent {
        return PendingIntent.getService(
            this, 0,
            Intent(this, PowerButtonService::class.java).apply {
                action = ACTION_CANCEL_SOS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}