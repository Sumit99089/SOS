package com.example.sos.network

data class RegisterRequest(val email: String, val name: String, val password: String)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val token: String, val userId: String)

data class SosRequest(
    val userId: String,
    val token: String,
    val latitude: Double,
    val longitude: Double
)