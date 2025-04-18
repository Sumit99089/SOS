package com.example.sos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class SosServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SosServiceRestart", "Service restart requested")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "com.example.sos.RESTART_SERVICE") {

            val serviceIntent = Intent(context, PowerButtonService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}