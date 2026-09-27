package com.notes.server.repository

import com.notes.server.models.UserAccount
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface UserRepository {
    fun createUser(email: String, passwordHash: String, displayName: String): Result<UserAccount>
    fun findByEmail(email: String): UserAccount?
    fun findById(userId: String): UserAccount?
    fun saveRefreshToken(refreshToken: String, userId: String)
    fun getUserIdForRefreshToken(refreshToken: String): String?
    fun revokeRefreshToken(refreshToken: String): Boolean
    fun clear()
}

class InMemoryUserRepository : UserRepository {
    private val usersById = ConcurrentHashMap<String, UserAccount>()
    private val emailToUserId = ConcurrentHashMap<String, String>()
    private val refreshTokens = ConcurrentHashMap<String, String>() // refreshToken -> userId

    override fun createUser(email: String, passwordHash: String, displayName: String): Result<UserAccount> {
        val normalizedEmail = email.trim().lowercase()
        if (emailToUserId.containsKey(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("User with this email already exists"))
        }

        val userId = "usr_" + UUID.randomUUID().toString()
        val account = UserAccount(
            userId = userId,
            email = normalizedEmail,
            displayName = displayName.ifBlank { normalizedEmail.substringBefore("@") },
            passwordHash = passwordHash,
            createdAt = System.currentTimeMillis()
        )

        val previous = emailToUserId.putIfAbsent(normalizedEmail, userId)
        if (previous != null) {
            return Result.failure(IllegalArgumentException("User with this email already exists"))
        }

        usersById[userId] = account
        return Result.success(account)
    }

    override fun findByEmail(email: String): UserAccount? {
        val normalizedEmail = email.trim().lowercase()
        val userId = emailToUserId[normalizedEmail] ?: return null
        return usersById[userId]
    }

    override fun findById(userId: String): UserAccount? {
        return usersById[userId]
    }

    override fun saveRefreshToken(refreshToken: String, userId: String) {
        refreshTokens[refreshToken] = userId
    }

    override fun getUserIdForRefreshToken(refreshToken: String): String? {
        return refreshTokens[refreshToken]
    }

    override fun revokeRefreshToken(refreshToken: String): Boolean {
        return refreshTokens.remove(refreshToken) != null
    }

    override fun clear() {
        usersById.clear()
        emailToUserId.clear()
        refreshTokens.clear()
    }
}

val defaultUserRepository: UserRepository = InMemoryUserRepository()
