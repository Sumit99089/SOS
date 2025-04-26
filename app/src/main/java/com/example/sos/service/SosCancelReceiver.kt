package com.example.sos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sos.service.PowerButtonService.Companion.ACTION_CANCEL_SOS

class CancelSosReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.startService(
            Intent(context, PowerButtonService::class.java).apply {
                action = ACTION_CANCEL_SOS
            }
        )
    }
}