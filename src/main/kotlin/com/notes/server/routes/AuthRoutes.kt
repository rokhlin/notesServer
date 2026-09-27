package com.notes.server.routes

import com.notes.server.models.AuthResponse
import com.notes.server.models.ErrorResponse
import com.notes.server.models.LoginRequest
import com.notes.server.models.RefreshResponse
import com.notes.server.models.RefreshTokenRequest
import com.notes.server.models.RegisterRequest
import com.notes.server.models.UserProfile
import com.notes.server.repository.UserRepository
import com.notes.server.repository.defaultUserRepository
import com.notes.server.security.JwtTokenManager
import com.notes.server.security.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRouting(userRepository: UserRepository = defaultUserRepository) {
    route("/api/v1/auth") {

        post("/register") {
            val request = runCatching { call.receive<RegisterRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid registration payload")
                )
            }

            val email = request.email.trim()
            if (!email.contains("@") || !email.contains(".")) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid email syntax")
                )
            }

            if (request.password.length < 8) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Password must be at least 8 characters")
                )
            }

            val passwordHash = PasswordHasher.hashPassword(request.password)
            val createResult = userRepository.createUser(
                email = email,
                passwordHash = passwordHash,
                displayName = request.displayName
            )

            createResult.onSuccess { user ->
                val accessToken = JwtTokenManager.generateAccessToken(
                    userId = user.userId,
                    email = user.email,
                    displayName = user.displayName
                )
                val refreshToken = JwtTokenManager.generateRefreshToken()
                userRepository.saveRefreshToken(refreshToken, user.userId)

                call.respond(
                    HttpStatusCode.Created,
                    AuthResponse(
                        userId = user.userId,
                        email = user.email,
                        displayName = user.displayName,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = JwtTokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                    )
                )
            }.onFailure { error ->
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse(error.message ?: "User registration conflict")
                )
            }
        }

        post("/login") {
            val request = runCatching { call.receive<LoginRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid login payload")
                )
            }

            val user = userRepository.findByEmail(request.email)
            if (user == null || !PasswordHasher.verifyPassword(request.password, user.passwordHash)) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password")
                )
            }

            val accessToken = JwtTokenManager.generateAccessToken(
                userId = user.userId,
                email = user.email,
                displayName = user.displayName
            )
            val refreshToken = JwtTokenManager.generateRefreshToken()
            userRepository.saveRefreshToken(refreshToken, user.userId)

            call.respond(
                HttpStatusCode.OK,
                AuthResponse(
                    userId = user.userId,
                    email = user.email,
                    displayName = user.displayName,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = JwtTokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                )
            )
        }

        post("/refresh") {
            val request = runCatching { call.receive<RefreshTokenRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid refresh token payload")
                )
            }

            val userId = userRepository.getUserIdForRefreshToken(request.refreshToken)
            if (userId == null) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid or expired refresh token")
                )
            }

            val user = userRepository.findById(userId)
            if (user == null) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Associated user account no longer exists")
                )
            }

            val newAccessToken = JwtTokenManager.generateAccessToken(
                userId = user.userId,
                email = user.email,
                displayName = user.displayName
            )

            call.respond(
                HttpStatusCode.OK,
                RefreshResponse(
                    accessToken = newAccessToken,
                    expiresIn = JwtTokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                )
            )
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                if (principal == null) {
                    return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Missing or invalid token principal")
                    )
                }

                val userId = principal.subject ?: ""
                val user = userRepository.findById(userId)
                if (user == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("User account not found")
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    UserProfile(
                        userId = user.userId,
                        email = user.email,
                        displayName = user.displayName,
                        createdAt = user.createdAt
                    )
                )
            }
        }
    }
}
