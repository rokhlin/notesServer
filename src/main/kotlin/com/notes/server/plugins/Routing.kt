package com.notes.server.plugins

import com.notes.server.routes.authRouting
import com.notes.server.routes.collabWebSocketRouting
import com.notes.server.routes.exportRouting
import com.notes.server.routes.notesRouting
import com.notes.server.routes.syncRouting
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(mapOf("service" to "notesServer", "status" to "running"))
        }

        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        authRouting()
        syncRouting()
        collabWebSocketRouting()
        exportRouting()
        notesRouting()
    }
}
