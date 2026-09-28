package com.notes.common.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class AiModelStatus {
    ACTIVE,
    DEPRECATED,
    SUNSET
}

@Serializable
enum class AiModelTier {
    PRIMARY,
    FALLBACK,
    FAST,
    PRO,
    EXPERIMENTAL
}

@Serializable
data class AiModelEntry(
    val id: String,
    val displayName: String,
    val tier: AiModelTier = AiModelTier.PRIMARY,
    val contextWindow: Int = 128000,
    val isRecommended: Boolean = false,
    val status: AiModelStatus = AiModelStatus.ACTIVE,
    val sunsetDate: String? = null
)

@Serializable
data class AiProviderCatalogEntry(
    val provider: AiProviderType,
    val baseUrl: String,
    val defaultPrimaryModelId: String,
    val defaultFallbackModelId: String? = null,
    val models: List<AiModelEntry> = emptyList()
)

@Serializable
data class AiModelCatalog(
    val schemaVersion: String = "1.0.0",
    val catalogVersion: String = "1.0.0",
    val updatedAt: String = "",
    val providers: Map<AiProviderType, AiProviderCatalogEntry> = emptyMap()
) {
    fun getModelsForProvider(providerType: AiProviderType): List<AiModelEntry> {
        return providers[providerType]?.models?.filter { it.status != AiModelStatus.SUNSET } ?: emptyList()
    }

    fun getPrimaryModel(providerType: AiProviderType): String {
        return providers[providerType]?.defaultPrimaryModelId ?: when (providerType) {
            AiProviderType.GEMINI -> "gemini-3.5-flash"
            AiProviderType.OPENAI -> "gpt-4o-mini"
            AiProviderType.ANTHROPIC -> "claude-3-5-haiku-20241022"
            AiProviderType.LOCAL_SERVER -> "llama3.3"
        }
    }

    fun getFallbackModel(providerType: AiProviderType): String? {
        return providers[providerType]?.defaultFallbackModelId ?: when (providerType) {
            AiProviderType.GEMINI -> "gemini-3.8-flash"
            AiProviderType.OPENAI -> "gpt-4o"
            AiProviderType.ANTHROPIC -> "claude-3-7-sonnet"
            AiProviderType.LOCAL_SERVER -> "llama3.2"
        }
    }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun loadFromJson(jsonString: String): AiModelCatalog {
            if (jsonString.isBlank()) return defaultCatalog()
            return try {
                jsonParser.decodeFromString<AiModelCatalog>(jsonString)
            } catch (_: Throwable) {
                defaultCatalog()
            }
        }

        fun defaultCatalog(): AiModelCatalog {
            return try {
                jsonParser.decodeFromString<AiModelCatalog>(DEFAULT_CATALOG_JSON)
            } catch (_: Throwable) {
                fallbackHardcodedCatalog()
            }
        }

        private fun fallbackHardcodedCatalog(): AiModelCatalog = AiModelCatalog(
            schemaVersion = "1.0.0",
            catalogVersion = "1.0.0",
            updatedAt = "2026-09-28T22:00:00Z",
            providers = mapOf(
                AiProviderType.GEMINI to AiProviderCatalogEntry(
                    provider = AiProviderType.GEMINI,
                    baseUrl = "https://generativelanguage.googleapis.com",
                    defaultPrimaryModelId = "gemini-3.5-flash",
                    defaultFallbackModelId = "gemini-3.8-flash",
                    models = listOf(
                        AiModelEntry("gemini-3.5-flash", "Gemini 3.5 Flash", AiModelTier.PRIMARY, 1048576, true),
                        AiModelEntry("gemini-3.8-flash", "Gemini 3.8 Flash", AiModelTier.FALLBACK, 1048576, true),
                        AiModelEntry("gemini-3.0-pro", "Gemini 3.0 Pro", AiModelTier.PRO, 2097152, false)
                    )
                ),
                AiProviderType.OPENAI to AiProviderCatalogEntry(
                    provider = AiProviderType.OPENAI,
                    baseUrl = "https://api.openai.com/v1",
                    defaultPrimaryModelId = "gpt-4o-mini",
                    defaultFallbackModelId = "gpt-4o",
                    models = listOf(
                        AiModelEntry("gpt-4o-mini", "GPT-4o Mini", AiModelTier.PRIMARY, 128000, true),
                        AiModelEntry("gpt-4o", "GPT-4o", AiModelTier.FALLBACK, 128000, true),
                        AiModelEntry("o3-mini", "o3-mini", AiModelTier.PRO, 200000, false)
                    )
                ),
                AiProviderType.ANTHROPIC to AiProviderCatalogEntry(
                    provider = AiProviderType.ANTHROPIC,
                    baseUrl = "https://api.anthropic.com/v1",
                    defaultPrimaryModelId = "claude-3-5-haiku-20241022",
                    defaultFallbackModelId = "claude-3-7-sonnet",
                    models = listOf(
                        AiModelEntry("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", AiModelTier.PRIMARY, 200000, true),
                        AiModelEntry("claude-3-7-sonnet", "Claude 3.7 Sonnet", AiModelTier.FALLBACK, 200000, true),
                        AiModelEntry("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", AiModelTier.PRO, 200000, false)
                    )
                ),
                AiProviderType.LOCAL_SERVER to AiProviderCatalogEntry(
                    provider = AiProviderType.LOCAL_SERVER,
                    baseUrl = "http://localhost:11434",
                    defaultPrimaryModelId = "llama3.3",
                    defaultFallbackModelId = "llama3.2",
                    models = listOf(
                        AiModelEntry("llama3.3", "Llama 3.3 (70B)", AiModelTier.PRIMARY, 128000, true),
                        AiModelEntry("llama3.2", "Llama 3.2 (3B)", AiModelTier.FALLBACK, 128000, true),
                        AiModelEntry("mistral-small", "Mistral Small 3", AiModelTier.FAST, 32000, false),
                        AiModelEntry("deepseek-r1", "DeepSeek R1 Distill", AiModelTier.PRO, 64000, false)
                    )
                )
            )
        )

        const val DEFAULT_CATALOG_JSON: String = """{
  "schemaVersion": "1.0.0",
  "catalogVersion": "1.0.0",
  "updatedAt": "2026-09-28T22:00:00Z",
  "providers": {
    "GEMINI": {
      "provider": "GEMINI",
      "baseUrl": "https://generativelanguage.googleapis.com",
      "defaultPrimaryModelId": "gemini-3.5-flash",
      "defaultFallbackModelId": "gemini-3.8-flash",
      "models": [
        {"id": "gemini-3.5-flash", "displayName": "Gemini 3.5 Flash", "tier": "PRIMARY", "contextWindow": 1048576, "isRecommended": true, "status": "ACTIVE"},
        {"id": "gemini-3.8-flash", "displayName": "Gemini 3.8 Flash", "tier": "FALLBACK", "contextWindow": 1048576, "isRecommended": true, "status": "ACTIVE"},
        {"id": "gemini-3.0-pro", "displayName": "Gemini 3.0 Pro", "tier": "PRO", "contextWindow": 2097152, "isRecommended": false, "status": "ACTIVE"}
      ]
    },
    "OPENAI": {
      "provider": "OPENAI",
      "baseUrl": "https://api.openai.com/v1",
      "defaultPrimaryModelId": "gpt-4o-mini",
      "defaultFallbackModelId": "gpt-4o",
      "models": [
        {"id": "gpt-4o-mini", "displayName": "GPT-4o Mini", "tier": "PRIMARY", "contextWindow": 128000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "gpt-4o", "displayName": "GPT-4o", "tier": "FALLBACK", "contextWindow": 128000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "o3-mini", "displayName": "o3-mini", "tier": "PRO", "contextWindow": 200000, "isRecommended": false, "status": "ACTIVE"}
      ]
    },
    "ANTHROPIC": {
      "provider": "ANTHROPIC",
      "baseUrl": "https://api.anthropic.com/v1",
      "defaultPrimaryModelId": "claude-3-5-haiku-20241022",
      "defaultFallbackModelId": "claude-3-7-sonnet",
      "models": [
        {"id": "claude-3-5-haiku-20241022", "displayName": "Claude 3.5 Haiku", "tier": "PRIMARY", "contextWindow": 200000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "claude-3-7-sonnet", "displayName": "Claude 3.7 Sonnet", "tier": "FALLBACK", "contextWindow": 200000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "claude-3-5-sonnet-20241022", "displayName": "Claude 3.5 Sonnet", "tier": "PRO", "contextWindow": 200000, "isRecommended": false, "status": "ACTIVE"}
      ]
    },
    "LOCAL_SERVER": {
      "provider": "LOCAL_SERVER",
      "baseUrl": "http://localhost:11434",
      "defaultPrimaryModelId": "llama3.3",
      "defaultFallbackModelId": "llama3.2",
      "models": [
        {"id": "llama3.3", "displayName": "Llama 3.3 (70B)", "tier": "PRIMARY", "contextWindow": 128000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "llama3.2", "displayName": "Llama 3.2 (3B)", "tier": "FALLBACK", "contextWindow": 128000, "isRecommended": true, "status": "ACTIVE"},
        {"id": "mistral-small", "displayName": "Mistral Small 3", "tier": "FAST", "contextWindow": 32000, "isRecommended": false, "status": "ACTIVE"},
        {"id": "deepseek-r1", "displayName": "DeepSeek R1 Distill", "tier": "PRO", "contextWindow": 64000, "isRecommended": false, "status": "ACTIVE"}
      ]
    }
  }
}"""
    }
}
