package com.example.sos.network.model

data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)