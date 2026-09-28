package com.notes.server.repository

import com.notes.common.models.StorageBackendType
import com.notes.common.models.StoragePathConfig
import com.notes.common.models.UserCloudConfig
import java.util.concurrent.ConcurrentHashMap

interface UserConfigRepository {
    fun getConfig(userId: String): UserCloudConfig
    fun saveConfig(config: UserCloudConfig): UserCloudConfig
    fun clear()
}

class InMemoryUserConfigRepository : UserConfigRepository {
    private val configsByUserId = ConcurrentHashMap<String, UserCloudConfig>()

    override fun getConfig(userId: String): UserCloudConfig {
        return configsByUserId.computeIfAbsent(userId) { id ->
            UserCloudConfig(
                userId = id,
                email = "",
                displayName = "",
                storagePaths = StoragePathConfig(
                    localVaultPath = "vault_$id",
                    remoteStorageUrl = "https://r2.notesalltogether.com/users/$id",
                    storageBackendType = StorageBackendType.NOTES_SERVER_SYNC,
                    autoSyncEnabled = true,
                    syncIntervalSeconds = 30
                ),
                enabledModules = listOf("core-editor", "skia-canvas"),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override fun saveConfig(config: UserCloudConfig): UserCloudConfig {
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        configsByUserId[config.userId] = updated
        return updated
    }

    override fun clear() {
        configsByUserId.clear()
    }
}

val defaultUserConfigRepository: UserConfigRepository = InMemoryUserConfigRepository()
