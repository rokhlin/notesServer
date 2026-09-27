package com.notes.server.export

import com.notes.server.models.ExportRequest
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

object DocxExportWorker {

    fun generateDocx(request: ExportRequest): ByteArray {
        val document = XWPFDocument()
        val outputStream = ByteArrayOutputStream()

        // 1. Title
        val titlePara = document.createParagraph().apply {
            alignment = ParagraphAlignment.LEFT
            spacingAfter = 120
        }
        val titleRun = titlePara.createRun().apply {
            isBold = true
            fontSize = 20
            color = "1E293B"
            setText(request.title)
        }

        // 2. Author and Timestamp
        val metaPara = document.createParagraph().apply {
            alignment = ParagraphAlignment.LEFT
            spacingAfter = 240
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val metaRun = metaPara.createRun().apply {
            isItalic = true
            fontSize = 9
            color = "64748B"
            setText("Author: ${request.author} | Generated: ${dateFormat.format(Date())}")
        }

        // 3. Body paragraphs
        val lines = request.content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    val p = document.createParagraph().apply {
                        spacingBefore = 180
                        spacingAfter = 60
                    }
                    p.createRun().apply {
                        isBold = true
                        fontSize = 14
                        color = "0F172A"
                        setText(trimmed.substring(2).trim())
                    }
                }
                trimmed.startsWith("## ") -> {
                    val p = document.createParagraph().apply {
                        spacingBefore = 140
                        spacingAfter = 40
                    }
                    p.createRun().apply {
                        isBold = true
                        fontSize = 12
                        color = "334155"
                        setText(trimmed.substring(3).trim())
                    }
                }
                trimmed.startsWith("- ") -> {
                    val p = document.createParagraph().apply {
                        spacingAfter = 40
                        indentationLeft = 360
                    }
                    p.createRun().apply {
                        fontSize = 11
                        color = "334155"
                        setText("• " + trimmed.substring(2).trim())
                    }
                }
                trimmed.isEmpty() -> {
                    val p = document.createParagraph().apply {
                        spacingAfter = 60
                    }
                    p.createRun().setText("")
                }
                else -> {
                    val p = document.createParagraph().apply {
                        spacingAfter = 60
                    }
                    p.createRun().apply {
                        fontSize = 11
                        color = "334155"
                        setText(trimmed)
                    }
                }
            }
        }

        // 4. Embedded Canvas Snapshot
        if (!request.canvasSnapshotBase64.isNullOrBlank()) {
            runCatching {
                val imageBytes = Base64.getDecoder().decode(request.canvasSnapshotBase64)
                val imgPara = document.createParagraph().apply {
                    spacingBefore = 200
                    alignment = ParagraphAlignment.CENTER
                }
                val imgRun = imgPara.createRun()
                ByteArrayInputStream(imageBytes).use { bis ->
                    imgRun.addPicture(
                        bis,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "canvas_snapshot.png",
                        Units.toEMU(450.0),
                        Units.toEMU(300.0)
                    )
                }
            }
        }

        document.write(outputStream)
        document.close()
        return outputStream.toByteArray()
    }
}
