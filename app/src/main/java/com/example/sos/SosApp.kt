package com.example.sos

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore


class SosApp : Application() {

        object dataStore: DataStore<Preferences> by preferencesDataStore("user_prefs")

}


