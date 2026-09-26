package com.notes.server.models

import kotlinx.serialization.Serializable

public typealias Note = com.notes.common.models.Note
public typealias NoteType = com.notes.common.models.NoteType

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val type: NoteType = NoteType.TEXT
)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val tags: List<String>? = null,
    val type: NoteType? = null
)
