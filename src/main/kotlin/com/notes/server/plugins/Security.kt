package com.notes.server.plugins

import com.auth0.jwt.JWT
import com.notes.server.security.JwtTokenManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "NotesAlltogether"
            verifier(
                JWT.require(JwtTokenManager.algorithm)
                    .withIssuer(JwtTokenManager.ISSUER)
                    .withAudience(JwtTokenManager.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject
                val email = credential.payload.getClaim("email").asString()
                if (!userId.isNullOrBlank() && !email.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token is invalid, expired, or missing")
                )
            }
        }
    }
}
