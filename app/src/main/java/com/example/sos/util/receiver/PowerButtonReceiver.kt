package com.example.sos.util.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.sos.util.SOSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerButtonReceiver : BroadcastReceiver() {
    companion object {
        private const val DETECTION_WINDOW_MS = 3000 // 3 seconds
        private const val PRESS_COUNT_THRESHOLD = 5
    }

    private val pressTimestamps = mutableListOf<Long>()
    private val handler = Handler(Looper.getMainLooper())
    private val sosManager = SOSManager()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON || intent.action == Intent.ACTION_SCREEN_OFF) {
            val currentTime = System.currentTimeMillis()
            pressTimestamps.add(currentTime)

            // Remove timestamps older than the detection window
            val cutoffTime = currentTime - DETECTION_WINDOW_MS
            pressTimestamps.removeAll { it < cutoffTime }

            // Check if we have enough presses within the window
            if (pressTimestamps.size >= PRESS_COUNT_THRESHOLD) {
                // Clear timestamps to prevent multiple triggers
                pressTimestamps.clear()

                // Trigger SOS
                CoroutineScope(Dispatchers.IO).launch {
                    sosManager.triggerSOS(context)
                }
            }
        }
    }
}
