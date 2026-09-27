package com.notes.server.export

import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import com.notes.server.models.ExportRequest
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

object PdfExportWorker {

    fun generatePdf(request: ExportRequest): ByteArray {
        val document = Document(PageSize.A4, 40f, 40f, 50f, 50f)
        val outputStream = ByteArrayOutputStream()

        PdfWriter.getInstance(document, outputStream)
        document.open()

        // 1. Title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, Color(30, 41, 59))
        val titlePara = Paragraph(request.title, titleFont).apply {
            spacingAfter = 6f
        }
        document.add(titlePara)

        // 2. Author and Timestamp
        val metaFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9f, Color(100, 116, 139))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val metaPara = Paragraph("Author: ${request.author} | Generated: ${dateFormat.format(Date())}", metaFont).apply {
            spacingAfter = 16f
        }
        document.add(metaPara)

        // 3. Body paragraphs
        val headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, Color(15, 23, 42))
        val bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color(51, 65, 85))

        val lines = request.content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    val p = Paragraph(trimmed.substring(2).trim(), headingFont).apply {
                        spacingBefore = 10f
                        spacingAfter = 4f
                    }
                    document.add(p)
                }
                trimmed.startsWith("## ") -> {
                    val p = Paragraph(trimmed.substring(3).trim(), headingFont).apply {
                        spacingBefore = 8f
                        spacingAfter = 3f
                    }
                    document.add(p)
                }
                trimmed.startsWith("- ") -> {
                    val p = Paragraph("• " + trimmed.substring(2).trim(), bodyFont).apply {
                        spacingAfter = 2f
                        indentationLeft = 14f
                    }
                    document.add(p)
                }
                trimmed.isEmpty() -> {
                    val spacer = Paragraph(" ", bodyFont).apply {
                        spacingAfter = 4f
                    }
                    document.add(spacer)
                }
                else -> {
                    val p = Paragraph(trimmed, bodyFont).apply {
                        spacingAfter = 4f
                    }
                    document.add(p)
                }
            }
        }

        // 4. Embedded Canvas Snapshot
        if (!request.canvasSnapshotBase64.isNullOrBlank()) {
            runCatching {
                val imageBytes = Base64.getDecoder().decode(request.canvasSnapshotBase64)
                val image = Image.getInstance(imageBytes)
                val pageWidth = PageSize.A4.width - 80f
                if (image.width > pageWidth) {
                    image.scaleToFit(pageWidth, 400f)
                }
                image.spacingBefore = 14f
                document.add(image)
            }
        }

        document.close()
        return outputStream.toByteArray()
    }
}
