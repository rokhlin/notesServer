package com.notes.server

import com.notes.server.collab.CollabRoom
import com.notes.server.collab.CollabRoomManager
import com.notes.server.collab.LockResult
import com.notes.server.models.CollabClientMessage
import com.notes.server.models.CollabServerMessage
import com.notes.server.models.Collaborator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollabWebSocketTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var roomManager: CollabRoomManager
    private lateinit var room: CollabRoom

    @BeforeTest
    fun setUp() {
        roomManager = CollabRoomManager()
        room = roomManager.getOrCreateRoom("note_collab_101")
    }

    @Test
    fun testSerializationClientMessages() {
        val join = CollabClientMessage.Join("client_1", "Alex")
        val encodedJoin = json.encodeToString<CollabClientMessage>(join)
        assertTrue(encodedJoin.contains("\"type\":\"Join\""))
        val decodedJoin = json.decodeFromString<CollabClientMessage>(encodedJoin)
        assertEquals(join, decodedJoin)

        val delta = CollabClientMessage.TextDelta("op_1", 3, "old", "new")
        val encodedDelta = json.encodeToString<CollabClientMessage>(delta)
        assertTrue(encodedDelta.contains("\"type\":\"TextDelta\""))
        val decodedDelta = json.decodeFromString<CollabClientMessage>(encodedDelta)
        assertEquals(delta, decodedDelta)

        val lock = CollabClientMessage.AcquireCanvasLock("client_1")
        val encodedLock = json.encodeToString<CollabClientMessage>(lock)
        assertTrue(encodedLock.contains("\"type\":\"AcquireCanvasLock\""))

        val snapshot = CollabClientMessage.CanvasSnapshot("client_1", "{\"layers\":[]}")
        val encodedSnap = json.encodeToString<CollabClientMessage>(snapshot)
        assertTrue(encodedSnap.contains("\"type\":\"CanvasSnapshot\""))
    }

    @Test
    fun testSerializationServerMessages() {
        val roomState = CollabServerMessage.RoomState(
            noteId = "note_101",
            activeUsers = listOf(Collaborator("c1", "Alex")),
            currentLockHolder = "c1"
        )
        val encoded = json.encodeToString<CollabServerMessage>(roomState)
        assertTrue(encoded.contains("\"type\":\"RoomState\""))
        val decoded = json.decodeFromString<CollabServerMessage>(encoded)
        assertEquals(roomState, decoded)

        val granted = CollabServerMessage.CanvasLockGranted("c1", 1758930000000L)
        val encodedGranted = json.encodeToString<CollabServerMessage>(granted)
        assertTrue(encodedGranted.contains("\"type\":\"CanvasLockGranted\""))

        val denied = CollabServerMessage.CanvasLockDenied("Canvas is locked", "c1")
        val encodedDenied = json.encodeToString<CollabServerMessage>(denied)
        assertTrue(encodedDenied.contains("\"type\":\"CanvasLockDenied\""))
    }

    @Test
    fun testExclusiveCanvasLockAcquisitionAndContention() {
        // 1. Initial state: unlocked
        assertNull(room.currentLockHolder)

        // 2. Client A acquires lock -> Granted
        val resultA = room.acquireLock("client_A")
        assertTrue(resultA is LockResult.Granted)
        assertEquals("client_A", room.currentLockHolder)

        // 3. Client B attempts to acquire lock -> Denied
        val resultB = room.acquireLock("client_B")
        assertTrue(resultB is LockResult.Denied)
        assertEquals("client_A", (resultB as LockResult.Denied).currentHolder)
        assertEquals("client_A", room.currentLockHolder)

        // 4. Client B attempts to release Client A's lock -> Rejected
        assertFalse(room.releaseLock("client_B"))
        assertEquals("client_A", room.currentLockHolder)

        // 5. Client A voluntarily releases lock -> Succeeded
        assertTrue(room.releaseLock("client_A"))
        assertNull(room.currentLockHolder)

        // 6. Client B can now acquire lock -> Granted
        val secondResultB = room.acquireLock("client_B")
        assertTrue(secondResultB is LockResult.Granted)
        assertEquals("client_B", room.currentLockHolder)
    }

    @Test
    fun testInvoluntaryLockReleaseOnClientDisconnect() = runBlocking {
        // Client A acquires lock
        room.acquireLock("client_A")
        room.collaborators["client_A"] = Collaborator("client_A", "Alex")
        assertEquals("client_A", room.currentLockHolder)

        // Client A disconnects (removeClient called)
        room.removeClient("client_A")

        // Lock must be cleared immediately
        assertNull(room.currentLockHolder)
        assertEquals(0L, room.lockExpiresAt)
        assertFalse(room.collaborators.containsKey("client_A"))
    }

    @Test
    fun testLockExpiration() {
        // Acquire lock with 0 duration (immediately expired)
        room.acquireLock("client_expired", durationMs = -100L)

        // Client B tries to acquire -> Should succeed because previous lock expired
        val resultB = room.acquireLock("client_B")
        assertTrue(resultB is LockResult.Granted)
        assertEquals("client_B", room.currentLockHolder)
    }
}
