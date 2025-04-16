package com.example.sos


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
        Intent(this, PowerButtonService::class.java).also { startService(it) }
        setContent {
            SOSTheme {
                SosNavHost()
            }
        }
    }
}
