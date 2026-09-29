package com.notes.common.crypto

import com.notes.common.crypto.PureCrypto.fromHex
import com.notes.common.crypto.PureCrypto.toHex
import com.notes.common.models.NoteType
import com.notes.common.models.ProtectedNoteMetadata
import com.notes.common.models.SelfContainedProtectedNote
import kotlinx.serialization.json.Json

object ProtectedNoteCodec {

    const val MAGIC_HEADER = "NA_PROTECTED_V1"
    const val APP_IDENTITY = "NotesAlltogetherClientApp"
    val APP_SIGNATURE: String by lazy {
        PureCrypto.sha256(APP_IDENTITY.encodeToByteArray()).toHex()
    }
    const val PAYLOAD_BOUNDARY = "---NA_PAYLOAD_BOUNDARY---"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun deriveCheckTag(password: String, saltHex: String): String {
        val saltBytes = saltHex.fromHex()
        var currentKey = PureCrypto.hmacSha256(saltBytes, password.encodeToByteArray())
        for (i in 0 until 1000) {
            currentKey = PureCrypto.hmacSha256(currentKey, password.encodeToByteArray())
        }
        return PureCrypto.hmacSha256(currentKey, "VERIFY_NOTE_PASSWORD".encodeToByteArray()).toHex()
    }

    fun verifyPassword(enteredPassword: String, metadata: ProtectedNoteMetadata): Boolean {
        val computed = deriveCheckTag(enteredPassword, metadata.saltHex)
        return PureCrypto.constantTimeEquals(computed.lowercase(), metadata.passwordCheckTagHex.lowercase())
    }

    fun pack(note: SelfContainedProtectedNote): String {
        val metadataJson = json.encodeToString(ProtectedNoteMetadata.serializer(), note.metadata)
        return buildString {
            append(MAGIC_HEADER).append("\n")
            append("SIGNATURE:").append(APP_SIGNATURE).append("\n")
            append(metadataJson).append("\n")
            append(PAYLOAD_BOUNDARY).append("\n")
            append(note.payloadContent)
        }
    }

    fun unpack(rawContent: String): SelfContainedProtectedNote {
        val lines = rawContent.lines()
        require(lines.isNotEmpty() && lines[0].trim() == MAGIC_HEADER) {
            "Invalid protected note: missing magic header $MAGIC_HEADER"
        }
        require(lines.size >= 4 && lines[1].trim() == "SIGNATURE:$APP_SIGNATURE") {
            "Invalid protected note: missing or invalid application signature"
        }

        val boundaryIndex = rawContent.indexOf(PAYLOAD_BOUNDARY)
        require(boundaryIndex != -1) {
            "Invalid protected note: missing payload boundary marker"
        }

        val metadataJson = lines[2].trim()
        val metadata = json.decodeFromString(ProtectedNoteMetadata.serializer(), metadataJson)
        val payloadStart = boundaryIndex + PAYLOAD_BOUNDARY.length + 1
        val payload = if (payloadStart < rawContent.length) rawContent.substring(payloadStart) else ""

        return SelfContainedProtectedNote(metadata = metadata, payloadContent = payload)
    }

    fun createProtectedNote(
        noteId: String,
        title: String,
        password: String,
        payloadContent: String,
        type: NoteType = NoteType.TEXT,
        passwordHint: String? = null,
        autoLockMinutes: Int = 5,
        saltHex: String = PureCrypto.sha256("SALT_$noteId".encodeToByteArray()).toHex()
    ): SelfContainedProtectedNote {
        val checkTag = deriveCheckTag(password, saltHex)
        val metadata = ProtectedNoteMetadata(
            noteId = noteId,
            title = title,
            type = type,
            isProtected = true,
            protectionAlgorithm = "PBKDF2_HMAC_SHA256_1000",
            saltHex = saltHex,
            passwordCheckTagHex = checkTag,
            passwordHint = passwordHint,
            autoLockTimeoutMinutes = autoLockMinutes,
            createdAt = 1727500000000L,
            updatedAt = 1727500000000L,
            version = 1L
        )
        return SelfContainedProtectedNote(metadata = metadata, payloadContent = payloadContent)
    }
}
