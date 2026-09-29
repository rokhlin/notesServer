package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
data class NoteMetadata(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val type: NoteType = NoteType.TEXT,
    val isEncrypted: Boolean = false,
    val isProtected: Boolean = false,
    val updatedAt: Long = 0L,
    val sizeBytes: Long = 0L
)

@Serializable
data class NotesIndexCatalog(
    val version: Int = 1,
    val lastSyncedAt: Long = 0L,
    val notes: List<NoteMetadata> = emptyList()
)

@Serializable
data class EncryptedPayload(
    val algorithm: String = "AES-GCM-256",
    val ivHex: String,
    val tagHex: String,
    val ciphertextBase64: String
)
