package com.notes.server.collab

import com.notes.server.models.CollabServerMessage
import com.notes.server.models.Collaborator
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

sealed class LockResult {
    data class Granted(val expiresAt: Long) : LockResult()
    data class Denied(val reason: String, val currentHolder: String) : LockResult()
}

class CollabRoom(val noteId: String) {
    val sessions = ConcurrentHashMap<String, WebSocketSession>()
    val collaborators = ConcurrentHashMap<String, Collaborator>()

    @Volatile
    var currentLockHolder: String? = null

    @Volatile
    var lockExpiresAt: Long = 0L

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun broadcast(message: CollabServerMessage, excludeClientId: String? = null) {
        val payload = json.encodeToString(CollabServerMessage.serializer(), message)
        val frame = Frame.Text(payload)
        for ((clientId, session) in sessions) {
            if (clientId != excludeClientId) {
                runCatching { session.send(frame) }
            }
        }
    }

    suspend fun sendTo(clientId: String, message: CollabServerMessage) {
        val session = sessions[clientId] ?: return
        val payload = json.encodeToString(CollabServerMessage.serializer(), message)
        runCatching { session.send(Frame.Text(payload)) }
    }

    fun acquireLock(clientId: String, durationMs: Long = 300_000L): LockResult {
        val now = System.currentTimeMillis()
        val holder = currentLockHolder
        if (holder == null || holder == clientId || (lockExpiresAt in 1..now)) {
            currentLockHolder = clientId
            lockExpiresAt = now + durationMs
            return LockResult.Granted(lockExpiresAt)
        }
        return LockResult.Denied("Canvas is currently locked by another collaborator", holder)
    }

    fun releaseLock(clientId: String): Boolean {
        if (currentLockHolder == clientId) {
            currentLockHolder = null
            lockExpiresAt = 0L
            return true
        }
        return false
    }

    suspend fun removeClient(clientId: String) {
        sessions.remove(clientId)
        collaborators.remove(clientId)
        if (currentLockHolder == clientId) {
            currentLockHolder = null
            lockExpiresAt = 0L
            broadcast(CollabServerMessage.CanvasLockReleased(previousHolder = clientId))
        }
        broadcast(CollabServerMessage.UserLeft(clientId = clientId))
    }
}

class CollabRoomManager {
    private val rooms = ConcurrentHashMap<String, CollabRoom>()
    private val protectedNoteIds = ConcurrentHashMap.newKeySet<String>()

    fun setProtected(noteId: String, isProtected: Boolean) {
        if (isProtected) {
            protectedNoteIds.add(noteId)
        } else {
            protectedNoteIds.remove(noteId)
        }
    }

    fun isProtected(noteId: String): Boolean {
        return noteId.contains("protected", ignoreCase = true) || protectedNoteIds.contains(noteId)
    }

    fun getOrCreateRoom(noteId: String): CollabRoom {
        return rooms.computeIfAbsent(noteId) { CollabRoom(noteId) }
    }

    fun getRoom(noteId: String): CollabRoom? {
        return rooms[noteId]
    }

    fun clear() {
        rooms.clear()
        protectedNoteIds.clear()
    }
}

val defaultCollabRoomManager = CollabRoomManager()
