package com.example.sos


import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.sos.service.PowerButtonService
import com.example.sos.ui.navigation.SosNavHost
import com.example.sos.ui.theme.SOSTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(application, PowerButtonService::class.java)
        application.startService(serviceIntent)
        setContent {
            SOSTheme {
                SosNavHost()
            }
        }
    }
}
