package com.notes.common.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonModelsServerTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    @Test
    fun testServerNoteSerialization() {
        val note = Note(
            id = "server-note-1",
            title = "Server Synchronized Note",
            content = "Testing cross-platform model synchronization.",
            type = NoteType.TEXT,
            tags = listOf("backend", "sync"),
            isEncrypted = false,
            createdAt = 1718000000000L,
            updatedAt = 1718000001000L,
            version = 2L
        )

        val encoded = json.encodeToString(note)
        val decoded = json.decodeFromString<Note>(encoded)

        assertEquals(note, decoded)
        assertEquals("Server Synchronized Note", decoded.title)
        assertEquals(2, decoded.tags.size)
    }

    @Test
    fun testServerSyncAndCanvasSerialization() {
        val manifest = CmnManifest(
            version = 1,
            noteId = "cmn-101",
            title = "Meeting Canvas",
            layers = listOf(
                CanvasLayer(
                    id = "layer-bg",
                    name = "Grid",
                    layerType = LayerType.BACKGROUND_GRID
                )
            )
        )
        val syncReq = SyncRequest(
            clientId = "web-client-1",
            lastSyncedTimestamp = 1718000000000L,
            modifiedNotes = emptyList(),
            deletedNoteIds = listOf("deleted-note-99")
        )
        val syncResp = SyncResponse(
            serverTimestamp = 1718000005000L,
            serverUpdates = listOf(Note(id = "srv-1", title = "New Note")),
            conflictsResolved = 0
        )

        val decodedManifest = json.decodeFromString<CmnManifest>(json.encodeToString(manifest))
        val decodedSyncReq = json.decodeFromString<SyncRequest>(json.encodeToString(syncReq))
        val decodedSyncResp = json.decodeFromString<SyncResponse>(json.encodeToString(syncResp))

        assertEquals(manifest, decodedManifest)
        assertEquals(syncReq, decodedSyncReq)
        assertEquals(syncResp, decodedSyncResp)
        assertEquals(1, decodedSyncResp.serverUpdates.size)
    }
}
