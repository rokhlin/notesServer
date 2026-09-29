package com.notes.common.models

import kotlinx.serialization.Serializable

object HmacHeaderConstants {
    const val HEADER_USER_KEY = "X-User-Key"
    const val HEADER_TIMESTAMP = "X-Timestamp"
    const val HEADER_NONCE = "X-Nonce"
    const val HEADER_SIGNATURE = "X-Signature"
    const val MAX_CLOCK_SKEW_MS = 300_000L // 5 minutes
}

@Serializable
data class UserAuthProfile(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val accessToken: String,
    val refreshToken: String,
    val userApiKey: String,
    val signingSecret: String,
    val expiresIn: Long
)
