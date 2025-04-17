package com.example.sos.network.model

data class SosRequest(
    val userId: String,
    val token: String,
    val latitude: Double,
    val longitude: Double
)
