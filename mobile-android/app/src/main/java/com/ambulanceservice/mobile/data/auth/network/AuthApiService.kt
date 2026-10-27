package com.ambulanceservice.mobile.data.auth.network

import com.ambulanceservice.mobile.data.auth.model.AuthResponse
import com.ambulanceservice.mobile.data.auth.model.LoginRequest
import com.ambulanceservice.mobile.data.auth.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
