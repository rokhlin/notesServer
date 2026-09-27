package com.notes.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Collaborator(
    val clientId: String,
    val displayName: String,
    val joinedAt: Long = System.currentTimeMillis()
)

@Serializable
sealed class CollabClientMessage {
    @Serializable
    @SerialName("Join")
    data class Join(val clientId: String, val displayName: String) : CollabClientMessage()

    @Serializable
    @SerialName("TextDelta")
    data class TextDelta(
        val operationId: String,
        val lineIndex: Int,
        val oldText: String,
        val newText: String
    ) : CollabClientMessage()

    @Serializable
    @SerialName("AcquireCanvasLock")
    data class AcquireCanvasLock(val clientId: String) : CollabClientMessage()

    @Serializable
    @SerialName("ReleaseCanvasLock")
    data class ReleaseCanvasLock(val clientId: String) : CollabClientMessage()

    @Serializable
    @SerialName("CanvasSnapshot")
    data class CanvasSnapshot(val clientId: String, val manifestJson: String) : CollabClientMessage()
}

@Serializable
sealed class CollabServerMessage {
    @Serializable
    @SerialName("RoomState")
    data class RoomState(
        val noteId: String,
        val activeUsers: List<Collaborator>,
        val currentLockHolder: String?
    ) : CollabServerMessage()

    @Serializable
    @SerialName("UserJoined")
    data class UserJoined(val clientId: String, val displayName: String) : CollabServerMessage()

    @Serializable
    @SerialName("UserLeft")
    data class UserLeft(val clientId: String) : CollabServerMessage()

    @Serializable
    @SerialName("TextDeltaBroadcast")
    data class TextDeltaBroadcast(
        val senderId: String,
        val operationId: String,
        val lineIndex: Int,
        val oldText: String,
        val newText: String,
        val timestamp: Long
    ) : CollabServerMessage()

    @Serializable
    @SerialName("CanvasLockGranted")
    data class CanvasLockGranted(val clientId: String, val expiresAt: Long) : CollabServerMessage()

    @Serializable
    @SerialName("CanvasLockDenied")
    data class CanvasLockDenied(val reason: String, val currentLockHolder: String) : CollabServerMessage()

    @Serializable
    @SerialName("CanvasLockReleased")
    data class CanvasLockReleased(val previousHolder: String) : CollabServerMessage()

    @Serializable
    @SerialName("CanvasSnapshotBroadcast")
    data class CanvasSnapshotBroadcast(val senderId: String, val manifestJson: String) : CollabServerMessage()

    @Serializable
    @SerialName("ErrorMessage")
    data class ErrorMessage(val message: String) : CollabServerMessage()
}
