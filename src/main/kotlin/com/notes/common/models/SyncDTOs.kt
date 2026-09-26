package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val clientId: String,
    val lastSyncedTimestamp: Long = 0L,
    val modifiedNotes: List<Note> = emptyList(),
    val deletedNoteIds: List<String> = emptyList()
)

@Serializable
data class SyncResponse(
    val serverTimestamp: Long = 0L,
    val serverUpdates: List<Note> = emptyList(),
    val serverDeletions: List<String> = emptyList(),
    val conflictsResolved: Int = 0
)

@Serializable
data class AuthRequest(
    val email: String,
    val passwordHash: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresIn: Long
)
