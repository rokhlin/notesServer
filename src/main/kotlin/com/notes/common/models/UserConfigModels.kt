package com.notes.common.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class StorageBackendType {
    LOCAL_DISK,
    CLOUDFLARE_R2,
    MINIO_S3,
    NOTES_SERVER_SYNC
}

@Serializable
data class StoragePathConfig(
    val localVaultPath: String = "default_vault",
    val remoteStorageUrl: String? = null,
    val storageBackendType: StorageBackendType = StorageBackendType.NOTES_SERVER_SYNC,
    val autoSyncEnabled: Boolean = false,
    val syncIntervalSeconds: Int = 30
)

@Serializable
data class UserCloudConfig(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val storagePaths: StoragePathConfig = StoragePathConfig(),
    val enabledModules: List<String> = listOf("core-editor", "skia-canvas"),
    val moduleLicenses: Map<String, String> = emptyMap(),
    val moduleUserConfigs: Map<String, JsonObject> = emptyMap(),
    val updatedAt: Long = 0L
)

@Serializable
data class DeviceLocalModuleConfig(
    val moduleId: String,
    val isHardwareAccelerationEnabled: Boolean = true,
    val stylusPressureCurve: Float = 1.0f,
    val localCacheDirectory: String = "",
    val maxLocalCacheBytes: Long = 524_288_000L,
    val deviceDensityScale: Float = 1.0f,
    val lastUpdated: Long = 0L
)
