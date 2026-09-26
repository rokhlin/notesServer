package com.notes.server.routes

import com.notes.server.models.CreateNoteRequest
import com.notes.server.models.Note
import com.notes.server.models.UpdateNoteRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val notesStorage = ConcurrentHashMap<String, Note>().apply {
    val initialNote = Note(
        id = UUID.randomUUID().toString(),
        title = "Welcome to NotesAlltogether",
        content = "This is a starter note from your Kotlin server. Edit or delete it as needed!"
    )
    put(initialNote.id, initialNote)
}

fun Route.notesRouting() {
    route("/api/notes") {
        get {
            call.respond(notesStorage.values.sortedByDescending { it.updatedAt })
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing note id")
            )
            val note = notesStorage[id] ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Note not found")
            )
            call.respond(note)
        }

        post {
            val request = runCatching { call.receive<CreateNoteRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid note payload")
                )
            }
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = request.title,
                content = request.content
            )
            notesStorage[newNote.id] = newNote
            call.respond(HttpStatusCode.Created, newNote)
        }

        put("{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing note id")
            )
            val existing = notesStorage[id] ?: return@put call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Note not found")
            )
            val request = runCatching { call.receive<UpdateNoteRequest>() }.getOrElse {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid update payload")
                )
            }
            val updated = existing.copy(
                title = request.title ?: existing.title,
                content = request.content ?: existing.content,
                updatedAt = System.currentTimeMillis()
            )
            notesStorage[id] = updated
            call.respond(updated)
        }

        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing note id")
            )
            if (notesStorage.remove(id) != null) {
                call.respond(HttpStatusCode.NoContent, "")
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Note not found")
                )
            }
        }
    }
}
