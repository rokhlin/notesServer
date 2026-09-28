package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
enum class AiProviderType {
    GEMINI,
    OPENAI,
    ANTHROPIC,
    LOCAL_SERVER
}

@Serializable
enum class LocalAiProtocol {
    OPENAI_COMPATIBLE,
    OLLAMA_NATIVE
}

@Serializable
data class AiProviderConfig(
    val providerType: AiProviderType = AiProviderType.GEMINI,
    val apiKey: String = "",
    val primaryModelId: String = "gemini-3.5-flash",
    val fallbackModelId: String? = "gemini-3.8-flash",
    val isFallbackEnabled: Boolean = true,
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val isEnabled: Boolean = true,
    val localProtocol: LocalAiProtocol = LocalAiProtocol.OPENAI_COMPATIBLE
)

@Serializable
data class AiSettingsConfig(
    val activeProvider: AiProviderType = AiProviderType.GEMINI,
    val providers: Map<AiProviderType, AiProviderConfig> = defaultProviders(),
    val autoSuggestOnNoteCreation: Boolean = false,
    val maxTagsToGenerate: Int = 5
) {
    companion object {
        fun defaultProviders(): Map<AiProviderType, AiProviderConfig> = mapOf(
            AiProviderType.GEMINI to AiProviderConfig(
                providerType = AiProviderType.GEMINI,
                primaryModelId = "gemini-3.5-flash",
                fallbackModelId = "gemini-3.8-flash",
                baseUrl = "https://generativelanguage.googleapis.com"
            ),
            AiProviderType.OPENAI to AiProviderConfig(
                providerType = AiProviderType.OPENAI,
                primaryModelId = "gpt-4o-mini",
                fallbackModelId = "gpt-4o",
                baseUrl = "https://api.openai.com/v1"
            ),
            AiProviderType.ANTHROPIC to AiProviderConfig(
                providerType = AiProviderType.ANTHROPIC,
                primaryModelId = "claude-3-5-haiku-20241022",
                fallbackModelId = "claude-3-7-sonnet",
                baseUrl = "https://api.anthropic.com/v1"
            ),
            AiProviderType.LOCAL_SERVER to AiProviderConfig(
                providerType = AiProviderType.LOCAL_SERVER,
                primaryModelId = "llama3.3",
                fallbackModelId = "llama3.2",
                baseUrl = "http://localhost:11434",
                localProtocol = LocalAiProtocol.OPENAI_COMPATIBLE
            )
        )
    }

    fun getActiveConfig(): AiProviderConfig {
        return providers[activeProvider] ?: AiProviderConfig(providerType = activeProvider)
    }

    fun updateProvider(config: AiProviderConfig): AiSettingsConfig {
        return copy(providers = providers + (config.providerType to config))
    }
}

@Serializable
data class AiMetadataRequest(
    val noteId: String,
    val title: String,
    val content: String,
    val existingTags: List<String> = emptyList(),
    val maxTags: Int = 5
)

@Serializable
data class NoteMetadataFill(
    val suggestedTitle: String? = null,
    val suggestedTags: List<String> = emptyList(),
    val summary: String? = null,
    val suggestedWikilinks: List<String> = emptyList(),
    val detectedLanguage: String = "en"
)

@Serializable
data class ConnectionTestResult(
    val isSuccess: Boolean,
    val latencyMs: Long = 0L,
    val modelName: String = "",
    val errorMessage: String? = null
)
