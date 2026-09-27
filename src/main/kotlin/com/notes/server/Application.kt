package com.notes.server

import com.notes.server.plugins.configureHTTP
import com.notes.server.plugins.configureRouting
import com.notes.server.plugins.configureSecurity
import com.notes.server.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureRouting()
}
