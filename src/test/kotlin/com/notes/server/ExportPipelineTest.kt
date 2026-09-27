package com.notes.server

import com.notes.server.export.DocxExportWorker
import com.notes.server.export.ImportService
import com.notes.server.export.PdfExportWorker
import com.notes.server.models.ExportRequest
import com.notes.server.models.ImportRequest
import com.notes.server.models.ImportResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportPipelineTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val samplePngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="

    @Test
    fun testPdfGenerationDirect() {
        val req = ExportRequest(
            title = "Architecture Spec",
            content = "# Executive Summary\nModern note-taking platform.\n\n## Stack\n- Kotlin\n- Ktor\n- OpenPDF",
            author = "Alex Morgan"
        )
        val pdf = PdfExportWorker.generatePdf(req)
        assertTrue(pdf.isNotEmpty())
        // PDF magic bytes %PDF-
        assertEquals('%'.code.toByte(), pdf[0])
        assertEquals('P'.code.toByte(), pdf[1])
        assertEquals('D'.code.toByte(), pdf[2])
        assertEquals('F'.code.toByte(), pdf[3])
    }

    @Test
    fun testPdfWithCanvasSnapshot() {
        val req = ExportRequest(
            title = "Canvas Note Export",
            content = "Handwritten drawing notes below:",
            author = "Elena Rostova",
            canvasSnapshotBase64 = samplePngBase64
        )
        val pdf = PdfExportWorker.generatePdf(req)
        assertTrue(pdf.isNotEmpty())
        assertEquals('%'.code.toByte(), pdf[0])
    }

    @Test
    fun testDocxGenerationDirect() {
        val req = ExportRequest(
            title = "Project Plan 2026",
            content = "# Roadmap\nPhase 1: Editor.\nPhase 2: Handwritten canvas.\n\n- Task A\n- Task B",
            author = "Alex Morgan",
            canvasSnapshotBase64 = samplePngBase64
        )
        val docx = DocxExportWorker.generateDocx(req)
        assertTrue(docx.isNotEmpty())
        // ZIP/DOCX magic bytes: 'P', 'K', 0x03, 0x04
        assertEquals('P'.code.toByte(), docx[0])
        assertEquals('K'.code.toByte(), docx[1])
        assertEquals(0x03.toByte(), docx[2])
        assertEquals(0x04.toByte(), docx[3])
    }

    @Test
    fun testImportTextParsing() {
        val raw = "# System Innovations\nEvaluating Kotlin Multiplatform performance across mobile.\nTags: #engineering #kmp #mobile"
        val req = ImportRequest(filename = "notes.md", rawContent = raw)
        val imported = ImportService.parseTextImport(req)

        assertEquals("System Innovations", imported.title)
        assertTrue(imported.tags.contains("engineering"))
        assertTrue(imported.tags.contains("kmp"))
        assertTrue(imported.tags.contains("mobile"))
    }

    @Test
    fun testExportPdfHttpRoute() = testApplication {
        application { module() }

        val req = ExportRequest(
            title = "Server Export Brief",
            content = "# Notes\nContent for PDF export.",
            author = "Tester"
        )

        val response = client.post("/api/v1/export/pdf") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(req))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Pdf, response.contentType())
        val disposition = response.headers[HttpHeaders.ContentDisposition]
        assertTrue(disposition != null && disposition.contains("Server_Export_Brief.pdf"))
        val bytes = response.bodyAsBytes()
        assertEquals('%'.code.toByte(), bytes[0])
    }

    @Test
    fun testExportDocxHttpRoute() = testApplication {
        application { module() }

        val req = ExportRequest(
            title = "Word Document Export",
            content = "# Word Headings\nContent inside Word.",
            author = "Tester"
        )

        val response = client.post("/api/v1/export/docx") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(req))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val disposition = response.headers[HttpHeaders.ContentDisposition]
        assertTrue(disposition != null && disposition.contains("Word_Document_Export.docx"))
        val bytes = response.bodyAsBytes()
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])
    }

    @Test
    fun testImportTextHttpRoute() = testApplication {
        application { module() }

        val req = ImportRequest(
            filename = "meeting.md",
            rawContent = "# Weekly Sprint\nAction items reviewed. #retro #agile"
        )

        val response = client.post("/api/v1/import/text") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(req))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val parsed = json.decodeFromString<ImportResponse>(response.bodyAsText())
        assertEquals("Weekly Sprint", parsed.title)
        assertTrue(parsed.tags.contains("retro"))
        assertTrue(parsed.tags.contains("agile"))
    }
}
