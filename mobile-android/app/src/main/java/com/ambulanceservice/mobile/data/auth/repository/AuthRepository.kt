package com.ambulanceservice.mobile.data.auth.repository

import com.ambulanceservice.mobile.data.auth.model.AuthResponse
import com.ambulanceservice.mobile.data.auth.model.LoginRequest
import com.ambulanceservice.mobile.data.auth.model.RegisterRequest
import com.ambulanceservice.mobile.data.auth.network.AuthApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepository(
    private val authApiService: AuthApiService,
    private val gson: Gson = Gson()
) {
    suspend fun register(request: RegisterRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.register(request)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "Registration failed.")
            }
        } catch (_: IOException) {
            AuthResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            AuthResult.Error(exception.message ?: "Unexpected registration error.")
        }
    }

    suspend fun login(request: LoginRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.login(request)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "Login failed.")
            }
        } catch (_: IOException) {
            AuthResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            AuthResult.Error(exception.message ?: "Unexpected login error.")
        }
    }

    private fun parseErrorMessage(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null

        return runCatching {
            val json = gson.fromJson(rawError, Map::class.java)
            val message = json["message"] as? String
            message ?: json["error"] as? String ?: rawError
        }.getOrElse { rawError }
    }
}

sealed interface AuthResult {
    data class Success(val auth: AuthResponse) : AuthResult
    data class Error(val message: String) : AuthResult
}
