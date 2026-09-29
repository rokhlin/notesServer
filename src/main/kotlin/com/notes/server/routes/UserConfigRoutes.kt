package com.notes.server.routes

import com.notes.common.models.UserCloudConfig
import com.notes.server.models.ErrorResponse
import com.notes.server.repository.UserConfigRepository
import com.notes.server.repository.defaultUserConfigRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class PresignedUrlRequest(
    val objectKey: String,
    val operation: String = "PUT" // PUT or GET
)

@Serializable
data class PresignedUrlResponse(
    val url: String,
    val objectKey: String,
    val expiresInSeconds: Int = 900
)

fun Route.userConfigRouting(configRepository: UserConfigRepository = defaultUserConfigRepository) {
    route("/api/v1/user") {
        authenticate("auth-jwt") {
            get("/config") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject
                if (userId.isNullOrBlank()) {
                    return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                }

                val config = configRepository.getConfig(userId)
                call.respond(HttpStatusCode.OK, config)
            }

            put("/config") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject
                if (userId.isNullOrBlank()) {
                    return@put call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                }

                val body = runCatching { call.receive<UserCloudConfig>() }.getOrElse {
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid configuration payload"))
                }

                // Enforce caller ownership
                val securedConfig = body.copy(userId = userId)
                val saved = configRepository.saveConfig(securedConfig)
                call.respond(HttpStatusCode.OK, saved)
            }

            post("/presigned-url") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject
                if (userId.isNullOrBlank()) {
                    return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                }

                val request = runCatching { call.receive<PresignedUrlRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid presigned URL request"))
                }

                val expectedPrefix = "users/$userId/"
                if (!request.objectKey.startsWith(expectedPrefix)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Cross-tenant access violation: key must start with $expectedPrefix")
                    )
                }

                val presignedUrl = "https://r2.notesalltogether.com/${request.objectKey}?token=mock_presigned_for_$userId"
                call.respond(
                    HttpStatusCode.OK,
                    PresignedUrlResponse(
                        url = presignedUrl,
                        objectKey = request.objectKey,
                        expiresInSeconds = 900
                    )
                )
            }
        }
    }
}
