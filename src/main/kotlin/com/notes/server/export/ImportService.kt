package com.notes.server.export

import com.notes.server.models.ImportRequest
import com.notes.server.models.ImportResponse

object ImportService {

    private val headerRegex = Regex("""^#\s+(.+)$""", RegexOption.MULTILINE)
    private val tagRegex = Regex("""#([a-zA-Z0-9_\-]+)""")

    fun parseTextImport(request: ImportRequest): ImportResponse {
        val raw = request.rawContent.trim()
        
        // 1. Extract title
        val titleMatch = headerRegex.find(raw)
        val extractedTitle = titleMatch?.groupValues?.get(1)?.trim()
        val title = when {
            !extractedTitle.isNullOrBlank() -> extractedTitle
            request.filename.isNotBlank() -> request.filename.substringBeforeLast(".")
            else -> "Imported Note"
        }

        // 2. Extract tags
        val tags = tagRegex.findAll(raw)
            .map { it.groupValues[1].lowercase() }
            .filter { it != title.lowercase() }
            .distinct()
            .toList()

        return ImportResponse(
            title = title,
            content = raw,
            tags = tags,
            type = "TEXT"
        )
    }
}
