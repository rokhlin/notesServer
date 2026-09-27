package com.notes.server.models

import kotlinx.serialization.Serializable

@Serializable
data class ExportRequest(
    val title: String,
    val content: String,
    val author: String = "Notes User",
    val canvasSnapshotBase64: String? = null
)

@Serializable
data class ImportRequest(
    val filename: String = "",
    val rawContent: String
)

@Serializable
data class ImportResponse(
    val title: String,
    val content: String,
    val tags: List<String>,
    val type: String = "TEXT"
)
