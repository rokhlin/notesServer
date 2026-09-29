package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
enum class NoteType {
    TEXT,
    CANVAS,
    HYBRID
}

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String = "",
    val type: NoteType = NoteType.TEXT,
    val tags: List<String> = emptyList(),
    val isEncrypted: Boolean = false,
    val isProtected: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
