package com.example.sos

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

// Define DataStore at the top level with context extension
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class SosApp : Application() {
        companion object {
                lateinit var instance: SosApp
                        private set
        }

        override fun onCreate() {
                super.onCreate()
                instance = this
        }
}