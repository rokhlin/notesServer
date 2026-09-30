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
data class CanvasShape(
    val id: String,
    val type: String, // "STRAIGHT_LINE", "RECTANGLE", "CIRCLE", "ELLIPSE", "TRIANGLE"
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val colorHex: String = "#4F46E5",
    val strokeWidth: Float = 3.0f,
    val lineStyle: String = "SOLID" // "SOLID", "DASHED"
)

@Serializable
data class CanvasTextBox(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float = 180f,
    val height: Float = 80f,
    val fontSize: Float = 16f,
    val colorHex: String = "#000000"
)

@Serializable
data class CanvasLayer(
    val id: String,
    val name: String,
    val zIndex: Int = 0,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val layerType: LayerType = LayerType.VECTOR,
    val strokes: List<InkStroke> = emptyList(),
    val shapes: List<CanvasShape> = emptyList(),
    val textBoxes: List<CanvasTextBox> = emptyList()
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
