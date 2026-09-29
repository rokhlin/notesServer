package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
data class ProtectedNoteMetadata(
    val noteId: String,
    val title: String,
    val type: NoteType = NoteType.TEXT,
    val isProtected: Boolean = true,
    val protectionAlgorithm: String = "PBKDF2_HMAC_SHA256_100000",
    val saltHex: String,
    val passwordCheckTagHex: String,
    val passwordHint: String? = null,
    val autoLockTimeoutMinutes: Int = 5,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val version: Long = 1L
)

@Serializable
data class SelfContainedProtectedNote(
    val metadata: ProtectedNoteMetadata,
    val payloadContent: String
)
