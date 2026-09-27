package com.notes.server.security

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    private const val LOG_ROUNDS = 12

    fun hashPassword(password: String): String {
        val salt = BCrypt.gensalt(LOG_ROUNDS)
        return BCrypt.hashpw(password, salt)
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (_: Exception) {
            false
        }
    }
}
