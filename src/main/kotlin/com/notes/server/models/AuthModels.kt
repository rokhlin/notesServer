package com.notes.server.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String = ""
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userApiKey: String = "",
    val signingSecret: String = ""
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val expiresIn: Long
)

@Serializable
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAt: Long,
    val userApiKey: String = ""
)

@Serializable
data class ErrorResponse(
    val error: String
)

data class UserAccount(
    val userId: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAt: Long,
    val userApiKey: String = "",
    val signingSecret: String = ""
)
