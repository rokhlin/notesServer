package com.notes.server.models

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String
)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null
)
