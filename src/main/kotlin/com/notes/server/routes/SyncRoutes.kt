package com.notes.server.routes

import com.notes.common.models.SyncRequest
import com.notes.server.models.ErrorResponse
import com.notes.server.repository.SyncRepository
import com.notes.server.repository.defaultSyncRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class EmptyTrashResponse(val purgedCount: Int)

fun Route.syncRouting(syncRepository: SyncRepository = defaultSyncRepository) {
    authenticate("auth-jwt") {
        route("/api/v1") {
            post("/sync") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject
                if (userId.isNullOrBlank()) {
                    return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Missing or invalid user principal")
                    )
                }

                val syncRequest = runCatching { call.receive<SyncRequest>() }.getOrElse {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid sync request payload")
                    )
                }

                val response = syncRepository.sync(userId, syncRequest)
                call.respond(HttpStatusCode.OK, response)
            }

            put("/sync/protected/{noteId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject
                if (userId.isNullOrBlank()) {
                    return@put call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
                }

                val noteId = call.parameters["noteId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing note id")
                )

                val containerRaw = call.receiveText()
                if (containerRaw.isBlank()) {
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Empty protected container payload"))
                }

                val unpacked = runCatching {
                    com.notes.common.crypto.ProtectedNoteCodec.unpack(containerRaw)
                }.getOrElse { err ->
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Corrupt protected container: ${err.message}"))
                }

                val note = com.notes.common.models.Note(
                    id = noteId,
                    title = unpacked.metadata.title,
                    content = containerRaw,
                    type = unpacked.metadata.type,
                    isProtected = true,
                    updatedAt = unpacked.metadata.updatedAt,
                    version = unpacked.metadata.version
                )
                syncRepository.saveNote(userId, note)

                call.respond(HttpStatusCode.OK, mapOf("status" to "SAVED_ATOMIC", "noteId" to noteId, "version" to note.version.toString()))
            }

            route("/trash") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Unauthorized")
                    )

                    val trashNotes = syncRepository.getTrashNotes(userId)
                    call.respond(HttpStatusCode.OK, trashNotes)
                }

                post("/{id}/restore") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Unauthorized")
                    )

                    val noteId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing note id")
                    )

                    val restored = syncRepository.restoreTrashNote(userId, noteId)
                    if (restored != null) {
                        call.respond(HttpStatusCode.OK, restored)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse("Note not found in trash")
                        )
                    }
                }

                delete("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Unauthorized")
                    )

                    val noteId = call.parameters["id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing note id")
                    )

                    if (syncRepository.purgeTrashNote(userId, noteId)) {
                        call.respond(HttpStatusCode.NoContent, "")
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse("Note not found")
                        )
                    }
                }

                delete {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.subject ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Unauthorized")
                    )

                    val count = syncRepository.emptyTrash(userId)
                    call.respond(HttpStatusCode.OK, EmptyTrashResponse(purgedCount = count))
                }
            }

            get("/notes/{id}/revisions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Unauthorized")
                )

                val noteId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing note id")
                )

                val revisions = syncRepository.getRevisions(userId, noteId)
                call.respond(HttpStatusCode.OK, revisions)
            }
        }
    }
}
