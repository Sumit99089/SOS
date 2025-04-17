package com.example.sos.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sos.dataStore
import com.example.sos.network.RetrofitInstance
import com.example.sos.network.model.LoginRequest
import com.example.sos.network.model.LoginResponse
import com.example.sos.network.model.RegisterRequest
import com.example.sos.network.model.SosRequest
import kotlinx.coroutines.flow.first

class UserRepository(private val context: Context) {
    // Access dataStore directly from the context using the extension property
    private val dataStore = context.dataStore
    private val KEY_TOKEN = stringPreferencesKey("KEY_TOKEN")
    private val KEY_USER_ID = stringPreferencesKey("KEY_USER_ID")

    suspend fun register(email: String, name: String, password: String) =
        RetrofitInstance.api.register(RegisterRequest(email, name, password))

    suspend fun login(email: String, password: String): LoginResponse? {
        val resp = RetrofitInstance.api.login(LoginRequest(email, password))
        if (resp.isSuccessful) resp.body()?.let { saveCredentials(it.userId, it.token) }
        return resp.body()
    }

    private suspend fun saveCredentials(userId: String, token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun getUserId(): String? {
        val preferences = dataStore.data.first()
        return preferences[KEY_USER_ID]
    }

    suspend fun getToken(): String? {
        val preferences = dataStore.data.first()
        return preferences[KEY_TOKEN]
    }

    suspend fun sendSos(latitude: Double, longitude: Double) {
        val userId = getUserId() ?: return
        val token = getToken() ?: return
        RetrofitInstance.api.sendSos(SosRequest(userId, token, latitude, longitude))
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}