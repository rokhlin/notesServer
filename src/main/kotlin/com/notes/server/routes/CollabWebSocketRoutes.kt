package com.notes.server.routes

import com.notes.server.collab.CollabRoomManager
import com.notes.server.collab.LockResult
import com.notes.server.collab.defaultCollabRoomManager
import com.notes.server.models.CollabClientMessage
import com.notes.server.models.CollabServerMessage
import com.notes.server.models.Collaborator
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json

fun Route.collabWebSocketRouting(roomManager: CollabRoomManager = defaultCollabRoomManager) {
    val json = Json { ignoreUnknownKeys = true }

    webSocket("/api/v1/ws/notes/{noteId}") {
        val noteId = call.parameters["noteId"] ?: run {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing noteId"))
            return@webSocket
        }

        val room = roomManager.getOrCreateRoom(noteId)
        var registeredClientId: String? = null

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val clientMsg = runCatching {
                        json.decodeFromString<CollabClientMessage>(text)
                    }.getOrNull()

                    when (clientMsg) {
                        is CollabClientMessage.Join -> {
                            registeredClientId = clientMsg.clientId
                            val collaborator = Collaborator(
                                clientId = clientMsg.clientId,
                                displayName = clientMsg.displayName
                            )
                            room.collaborators[clientMsg.clientId] = collaborator
                            room.sessions[clientMsg.clientId] = this

                            // 1. Send RoomState to joiner
                            val roomState = CollabServerMessage.RoomState(
                                noteId = noteId,
                                activeUsers = room.collaborators.values.toList(),
                                currentLockHolder = room.currentLockHolder
                            )
                            send(Frame.Text(json.encodeToString(CollabServerMessage.serializer(), roomState)))

                            // 2. Broadcast UserJoined to peers
                            room.broadcast(
                                CollabServerMessage.UserJoined(
                                    clientId = clientMsg.clientId,
                                    displayName = clientMsg.displayName
                                ),
                                excludeClientId = clientMsg.clientId
                            )
                        }

                        is CollabClientMessage.TextDelta -> {
                            val senderId = registeredClientId ?: "anonymous"
                            val broadcast = CollabServerMessage.TextDeltaBroadcast(
                                senderId = senderId,
                                operationId = clientMsg.operationId,
                                lineIndex = clientMsg.lineIndex,
                                oldText = clientMsg.oldText,
                                newText = clientMsg.newText,
                                timestamp = System.currentTimeMillis()
                            )
                            room.broadcast(broadcast, excludeClientId = senderId)
                        }

                        is CollabClientMessage.AcquireCanvasLock -> {
                            val clientId = registeredClientId ?: clientMsg.clientId
                            when (val result = room.acquireLock(clientId)) {
                                is LockResult.Granted -> {
                                    room.broadcast(
                                        CollabServerMessage.CanvasLockGranted(
                                            clientId = clientId,
                                            expiresAt = result.expiresAt
                                        )
                                    )
                                }
                                is LockResult.Denied -> {
                                    room.sendTo(
                                        clientId,
                                        CollabServerMessage.CanvasLockDenied(
                                            reason = result.reason,
                                            currentLockHolder = result.currentHolder
                                        )
                                    )
                                }
                            }
                        }

                        is CollabClientMessage.ReleaseCanvasLock -> {
                            val clientId = registeredClientId ?: clientMsg.clientId
                            if (room.releaseLock(clientId)) {
                                room.broadcast(
                                    CollabServerMessage.CanvasLockReleased(previousHolder = clientId)
                                )
                            }
                        }

                        is CollabClientMessage.CanvasSnapshot -> {
                            val senderId = registeredClientId ?: clientMsg.clientId
                            room.broadcast(
                                CollabServerMessage.CanvasSnapshotBroadcast(
                                    senderId = senderId,
                                    manifestJson = clientMsg.manifestJson
                                ),
                                excludeClientId = senderId
                            )
                        }

                        null -> {
                            send(Frame.Text(json.encodeToString(
                                CollabServerMessage.serializer(),
                                CollabServerMessage.ErrorMessage("Invalid message payload")
                            )))
                        }
                    }
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            // Normal close
        } catch (_: Throwable) {
            // Socket error
        } finally {
            registeredClientId?.let { id ->
                room.removeClient(id)
            }
        }
    }
}
