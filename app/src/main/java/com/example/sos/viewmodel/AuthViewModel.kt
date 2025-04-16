package com.example.sos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sos.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application): AndroidViewModel(application) {
    private val repo = UserRepository(application)

    fun register(email: String, name: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resp = repo.register(email, name, password)
            onResult(resp.isSuccessful)
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resp = repo.login(email, password)
            onResult(resp != null)
        }
    }

    fun sendSos(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repo.sendSos(latitude, longitude)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onComplete()
        }
    }
}