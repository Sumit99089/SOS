package com.example.sos.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sos.SosApp
import com.example.sos.network.LoginRequest
import com.example.sos.network.LoginResponse
import com.example.sos.network.RegisterRequest
import com.example.sos.network.RetrofitInstance
import com.example.sos.network.SosRequest
import kotlinx.coroutines.flow.first

class UserRepository(private val context: Context) {
    private val sos = SosApp()
    private val dataStore = SosApp.dataStore
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
        dataStore.value.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun getUserId(): String? = dataStore.data.first()[KEY_USER_ID]
    suspend fun getToken(): String? = dataStore.data.first()[KEY_TOKEN]

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