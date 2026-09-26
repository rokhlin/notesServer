package com.notes.common.models

import kotlinx.serialization.Serializable

@Serializable
enum class ToolType {
    PEN,
    FOUNTAIN_PEN,
    PENCIL,
    CALLIGRAPHY_BRUSH,
    HIGHLIGHTER,
    VECTOR_ERASER
}

@Serializable
enum class LayerType {
    VECTOR,
    BACKGROUND_GRID,
    RASTER_IMAGE
}

@Serializable
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0.0f,
    val timestamp: Long = 0L
)

@Serializable
data class InkStroke(
    val id: String,
    val tool: ToolType = ToolType.PEN,
    val colorHex: String = "#000000",
    val strokeWidth: Float = 2.0f,
    val opacity: Float = 1.0f,
    val points: List<InkPoint> = emptyList()
)

@Serializable
data class CanvasLayer(
    val id: String,
    val name: String,
    val zIndex: Int = 0,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val layerType: LayerType = LayerType.VECTOR,
    val strokes: List<InkStroke> = emptyList()
)

@Serializable
data class CmnManifest(
    val version: Int = 1,
    val noteId: String,
    val title: String,
    val layers: List<CanvasLayer> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
