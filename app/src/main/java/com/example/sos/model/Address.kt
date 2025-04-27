package com.example.sos.model

data class Address(
    val line1: String = "",
    val line2: String = "",
    val landmark: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: Int? = null
)