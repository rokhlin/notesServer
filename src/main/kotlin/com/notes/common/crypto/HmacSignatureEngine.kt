package com.notes.common.crypto

import com.notes.common.crypto.PureCrypto.toHex

object HmacSignatureEngine {

    fun computeBodyHash(body: String): String {
        return PureCrypto.sha256(body.encodeToByteArray()).toHex()
    }

    fun computeSignature(
        method: String,
        path: String,
        timestamp: Long,
        nonce: String,
        bodyHash: String,
        secret: String
    ): String {
        val canonicalPayload = "${method.uppercase()}\n$path\n$timestamp\n$nonce\n$bodyHash"
        val keyBytes = secret.encodeToByteArray()
        val payloadBytes = canonicalPayload.encodeToByteArray()
        return PureCrypto.hmacSha256(keyBytes, payloadBytes).toHex()
    }

    fun verifySignature(
        method: String,
        path: String,
        timestamp: Long,
        nonce: String,
        bodyHash: String,
        secret: String,
        expectedSignature: String
    ): Boolean {
        val actual = computeSignature(method, path, timestamp, nonce, bodyHash, secret)
        return PureCrypto.constantTimeEquals(actual.lowercase(), expectedSignature.lowercase())
    }
}
