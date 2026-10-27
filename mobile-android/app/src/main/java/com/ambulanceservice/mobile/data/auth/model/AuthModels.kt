package com.ambulanceservice.mobile.data.auth.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val role: String,
    val ambulanceRegistrationNumber: String? = null,
    val address: String? = null,
    val inviteCode: String? = null
)

data class AuthResponse(
    val token: String? = null,
    val tokenType: String? = null,
    val userId: Long? = null,
    val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val status: String? = null
)

enum class AuthRole {
    USER,
    DRIVER
}

fun AuthRole.backendValue(): String = when (this) {
    AuthRole.USER -> "ROLE_USER"
    AuthRole.DRIVER -> "ROLE_DRIVER"
}
