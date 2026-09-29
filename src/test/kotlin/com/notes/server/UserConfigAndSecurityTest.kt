package com.notes.server

import com.notes.common.crypto.ProtectedNoteCodec
import com.notes.common.models.NoteType
import com.notes.common.models.StorageBackendType
import com.notes.common.models.StoragePathConfig
import com.notes.common.models.UserCloudConfig
import com.notes.server.collab.defaultCollabRoomManager
import com.notes.server.models.AuthResponse
import com.notes.server.models.LoginRequest
import com.notes.server.models.RegisterRequest
import com.notes.server.repository.defaultSyncRepository
import com.notes.server.repository.defaultUserConfigRepository
import com.notes.server.repository.defaultUserRepository
import com.notes.server.routes.PresignedUrlRequest
import com.notes.server.routes.PresignedUrlResponse
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserConfigAndSecurityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        defaultUserRepository.clear()
        defaultUserConfigRepository.clear()
        defaultSyncRepository.clear()
        defaultCollabRoomManager.clear()
    }

    @Test
    fun testUserRegistrationAndLoginReturnsApiKeyAndSecret() = testApplication {
        application { module() }

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("alice@example.com", "Password123!", "Alice")))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val auth1 = json.decodeFromString<AuthResponse>(registerResponse.bodyAsText())
        assertTrue(auth1.userApiKey.startsWith("uak_"), "Must return userApiKey")
        assertTrue(auth1.signingSecret.startsWith("sec_"), "Must return signingSecret")

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest("alice@example.com", "Password123!")))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val auth2 = json.decodeFromString<AuthResponse>(loginResponse.bodyAsText())
        assertEquals(auth1.userApiKey, auth2.userApiKey)
        assertEquals(auth1.signingSecret, auth2.signingSecret)
    }

    @Test
    fun testUserConfigGetAndPut() = testApplication {
        application { module() }

        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("bob@example.com", "Password123!", "Bob")))
        }
        val auth = json.decodeFromString<AuthResponse>(reg.bodyAsText())

        // 1. GET initial config
        val getRes = client.get("/api/v1/user/config") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, getRes.status)
        val config1 = json.decodeFromString<UserCloudConfig>(getRes.bodyAsText())
        assertEquals(auth.userId, config1.userId)

        // 2. PUT updated config
        val updatedConfig = config1.copy(
            storagePaths = StoragePathConfig(
                localVaultPath = "custom/local/path",
                remoteStorageUrl = "https://r2.custom.com",
                storageBackendType = StorageBackendType.CLOUDFLARE_R2,
                autoSyncEnabled = true
            ),
            enabledModules = listOf("core-editor", "skia-canvas", "premium-tables")
        )
        val putRes = client.put("/api/v1/user/config") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updatedConfig))
        }
        assertEquals(HttpStatusCode.OK, putRes.status)
        val config2 = json.decodeFromString<UserCloudConfig>(putRes.bodyAsText())
        assertEquals("custom/local/path", config2.storagePaths.localVaultPath)
        assertEquals(StorageBackendType.CLOUDFLARE_R2, config2.storagePaths.storageBackendType)
        assertTrue(config2.enabledModules.contains("premium-tables"))
    }

    @Test
    fun testPresignedUrlMultiTenantIsolation() = testApplication {
        application { module() }

        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("charlie@example.com", "Password123!", "Charlie")))
        }
        val auth = json.decodeFromString<AuthResponse>(reg.bodyAsText())

        // 1. Valid user path -> 200 OK
        val validReq = PresignedUrlRequest("users/${auth.userId}/notes/note_1.md")
        val validRes = client.post("/api/v1/user/presigned-url") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(validReq))
        }
        assertEquals(HttpStatusCode.OK, validRes.status)
        val presigned = json.decodeFromString<PresignedUrlResponse>(validRes.bodyAsText())
        assertTrue(presigned.url.contains("users/${auth.userId}/notes/note_1.md"))

        // 2. Cross-tenant path -> 403 Forbidden
        val invalidReq = PresignedUrlRequest("users/usr_victim_456/notes/note_secret.md")
        val invalidRes = client.post("/api/v1/user/presigned-url") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(invalidReq))
        }
        assertEquals(HttpStatusCode.Forbidden, invalidRes.status)
        assertTrue(invalidRes.bodyAsText().contains("Cross-tenant access violation"))
    }

    @Test
    fun testProtectedNoteAtomicSync() = testApplication {
        application { module() }

        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("david@example.com", "Password123!", "David")))
        }
        val auth = json.decodeFromString<AuthResponse>(reg.bodyAsText())

        // Create self-contained protected note
        val protectedNote = ProtectedNoteCodec.createProtectedNote(
            noteId = "prot_100",
            title = "Top Secret Plan",
            password = "SecretPassword!",
            payloadContent = "# Confidential Plan\n\nNo diffs, atomic full file only.",
            type = NoteType.TEXT,
            passwordHint = "My password"
        )
        val packed = ProtectedNoteCodec.pack(protectedNote)

        // 1. Upload valid protected note container
        val syncRes = client.put("/api/v1/sync/protected/prot_100") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            setBody(packed)
        }
        assertEquals(HttpStatusCode.OK, syncRes.status)
        assertTrue(syncRes.bodyAsText().contains("SAVED_ATOMIC"))

        // Verify it was saved in syncRepository
        val saved = defaultSyncRepository.getNote(auth.userId, "prot_100")
        assertNotNull(saved)
        assertTrue(saved.isProtected)
        assertEquals("Top Secret Plan", saved.title)

        // 2. Corrupted container fails with 400
        val corruptRes = client.put("/api/v1/sync/protected/prot_101") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
            setBody("CORRUPTED_GARBAGE_PAYLOAD")
        }
        assertEquals(HttpStatusCode.BadRequest, corruptRes.status)
        assertTrue(corruptRes.bodyAsText().contains("Corrupt protected container"))
    }

    @Test
    fun testProtectedNoteCollabRejection() = testApplication {
        val client = createClient {
            install(WebSockets)
        }
        application { module() }

        // Attempting to open a WebSocket room for a protected note ID must be rejected immediately
        client.webSocket("/api/v1/ws/notes/protected_note_financial_plans") {
            val incomingFrame = incoming.receive() as Frame.Text
            val text = incomingFrame.readText()
            assertTrue(
                text.contains("PROTECTED_NOTE_COLLAB_DISABLED"),
                "Must reject collaboration on protected notes with error message"
            )
        }
    }
}
