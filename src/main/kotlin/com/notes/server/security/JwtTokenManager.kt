package com.notes.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date
import java.util.UUID

object JwtTokenManager {
    const val ISSUER = "notesServer"
    const val AUDIENCE = "notesClientApp"
    private const val DEFAULT_SECRET = "notes-alltogether-jwt-secret-key-2026-production-grade"
    
    var secret: String = DEFAULT_SECRET
        private set

    val algorithm: Algorithm
        get() = Algorithm.HMAC256(secret)

    const val ACCESS_TOKEN_EXPIRATION_SECONDS = 3600L // 1 hour

    fun configureSecret(newSecret: String) {
        secret = newSecret
    }

    fun generateAccessToken(userId: String, email: String, displayName: String): String {
        val now = Date()
        val expiresAt = Date(now.time + ACCESS_TOKEN_EXPIRATION_SECONDS * 1000)

        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userId)
            .withClaim("email", email)
            .withClaim("name", displayName)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun generateRefreshToken(): String {
        return "ref_" + UUID.randomUUID().toString().replace("-", "")
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .build()
                .verify(token)
        } catch (_: Exception) {
            null
        }
    }
}
