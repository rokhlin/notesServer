package com.notes.server

import com.notes.common.models.Note
import com.notes.common.models.NoteType
import com.notes.common.models.SyncRequest
import com.notes.common.models.SyncResponse
import com.notes.server.models.AuthResponse
import com.notes.server.models.RegisterRequest
import com.notes.server.repository.defaultSyncRepository
import com.notes.server.repository.defaultUserRepository
import com.notes.server.routes.EmptyTrashResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncAndRecycleBinTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        defaultUserRepository.clear()
        defaultSyncRepository.clear()
    }

    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient, email: String): AuthResponse {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest(email, "StrongPass2026!", "Sync User")))
        }
        return json.decodeFromString(response.bodyAsText())
    }

    @Test
    fun testDeltaSyncInitialUploadAndDownload() = testApplication {
        application { module() }
        val auth = registerAndGetToken(client, "sync1@example.com")

        val clientNote = Note(
            id = "note_alpha",
            title = "First Sync Note",
            content = "Hello delta sync!",
            type = NoteType.TEXT,
            tags = listOf("sync", "test"),
            updatedAt = 1000L
        )

        val syncReq = SyncRequest(
            clientId = "device_A",
            lastSyncedTimestamp = 0L,
            modifiedNotes = listOf(clientNote),
            deletedNoteIds = emptyList()
        )

        val response = client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(syncReq))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val syncResp = json.decodeFromString<SyncResponse>(response.bodyAsText())
        assertTrue(syncResp.serverTimestamp > 0)
        assertEquals(0, syncResp.conflictsResolved)
        // Since clientNote was uploaded, it is present in server updates
        assertEquals(1, syncResp.serverUpdates.size)
        assertEquals("note_alpha", syncResp.serverUpdates[0].id)
    }

    @Test
    fun testLwwClientWinsWhenNewer() = testApplication {
        application { module() }
        val auth = registerAndGetToken(client, "lww_client@example.com")

        // 1. Initial note at t=1000
        val v1 = Note(
            id = "note_lww",
            title = "Version 1",
            content = "Original",
            updatedAt = 1000L
        )
        client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 0L, listOf(v1))))
        }

        // 2. Client update at t=2000
        val v2 = Note(
            id = "note_lww",
            title = "Version 2 Client",
            content = "Client edited content",
            updatedAt = 2000L
        )
        val syncResp2 = client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 1000L, listOf(v2))))
        }
        assertEquals(HttpStatusCode.OK, syncResp2.status)
        val resp2 = json.decodeFromString<SyncResponse>(syncResp2.bodyAsText())
        assertEquals(0, resp2.conflictsResolved)
        assertEquals(1, resp2.serverUpdates.size)
        assertEquals("Version 2 Client", resp2.serverUpdates[0].title)

        // 3. Inspect revisions
        val revResp = client.get("/api/v1/notes/note_lww/revisions") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, revResp.status)
        val revisions = json.decodeFromString<List<Note>>(revResp.bodyAsText())
        assertEquals(1, revisions.size)
        assertEquals("Version 1", revisions[0].title)
    }

    @Test
    fun testLwwServerWinsWhenNewerConflict() = testApplication {
        application { module() }
        val auth = registerAndGetToken(client, "lww_server@example.com")

        // 1. Server already has note with updatedAt = 5000
        val serverNote = Note(
            id = "note_conflict",
            title = "Server Authority",
            content = "Server latest content",
            updatedAt = 5000L
        )
        client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("dev_server", 0L, listOf(serverNote))))
        }

        // 2. Client sends an older version with updatedAt = 2000
        val staleClientNote = Note(
            id = "note_conflict",
            title = "Stale Edit",
            content = "Stale content",
            updatedAt = 2000L
        )
        val conflictResp = client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("dev_client", 1000L, listOf(staleClientNote))))
        }
        assertEquals(HttpStatusCode.OK, conflictResp.status)
        val resp = json.decodeFromString<SyncResponse>(conflictResp.bodyAsText())
        assertEquals(1, resp.conflictsResolved)
        assertEquals(1, resp.serverUpdates.size)
        assertEquals("Server Authority", resp.serverUpdates[0].title)
    }

    @Test
    fun testSoftDeleteToRecycleBinAndRestore() = testApplication {
        application { module() }
        val auth = registerAndGetToken(client, "trash@example.com")

        // 1. Create a note
        val note = Note(
            id = "note_trash_1",
            title = "To Be Deleted",
            content = "Delete me soon",
            updatedAt = 1000L
        )
        client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 0L, listOf(note))))
        }

        // 2. Soft-delete via sync deletedNoteIds
        val delSync = client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 1000L, emptyList(), listOf("note_trash_1"))))
        }
        val delResp = json.decodeFromString<SyncResponse>(delSync.bodyAsText())
        assertTrue(delResp.serverDeletions.contains("note_trash_1"))

        // 3. Check Recycle Bin
        val trashResp = client.get("/api/v1/trash") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, trashResp.status)
        val trashNotes = json.decodeFromString<List<Note>>(trashResp.bodyAsText())
        assertEquals(1, trashNotes.size)
        assertEquals("note_trash_1", trashNotes[0].id)
        assertTrue(trashNotes[0].isDeleted)
        assertNotNull(trashNotes[0].deletedAt)

        // 4. Restore the note from trash
        val restoreResp = client.post("/api/v1/trash/note_trash_1/restore") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, restoreResp.status)
        val restoredNote = json.decodeFromString<Note>(restoreResp.bodyAsText())
        assertFalse(restoredNote.isDeleted)
        assertEquals(null, restoredNote.deletedAt)

        // 5. Verify trash is now empty
        val emptyTrashResp = client.get("/api/v1/trash") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        val emptyTrashNotes = json.decodeFromString<List<Note>>(emptyTrashResp.bodyAsText())
        assertEquals(0, emptyTrashNotes.size)
    }

    @Test
    fun testPurgeAndEmptyTrash() = testApplication {
        application { module() }
        val auth = registerAndGetToken(client, "empty_trash@example.com")

        // 1. Create two notes and delete them
        val n1 = Note(id = "trash_a", title = "Trash A", updatedAt = 1000L)
        val n2 = Note(id = "trash_b", title = "Trash B", updatedAt = 1000L)
        client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 0L, listOf(n1, n2))))
        }
        client.post("/api/v1/sync") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest("devA", 1000L, emptyList(), listOf("trash_a", "trash_b"))))
        }

        // 2. Permanently purge trash_a
        val purgeResp = client.delete("/api/v1/trash/trash_a") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.NoContent, purgeResp.status)

        // 3. Empty remaining trash
        val emptyResp = client.delete("/api/v1/trash") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, emptyResp.status)
        val emptyResult = json.decodeFromString<EmptyTrashResponse>(emptyResp.bodyAsText())
        assertEquals(1, emptyResult.purgedCount) // trash_b was purged
    }
}
