package com.example.sos.network


import com.example.sos.network.model.LoginRequest
import com.example.sos.network.model.LoginResponse
import com.example.sos.network.model.RegisterRequest
import com.example.sos.network.model.SosRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("sos")
    suspend fun sendSos(@Body request: SosRequest): Response<Void>
}