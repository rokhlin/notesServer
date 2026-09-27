package com.notes.server.routes

import com.notes.server.export.DocxExportWorker
import com.notes.server.export.ImportService
import com.notes.server.export.PdfExportWorker
import com.notes.server.models.ErrorResponse
import com.notes.server.models.ExportRequest
import com.notes.server.models.ImportRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.exportRouting() {
    route("/api/v1") {

        post("/export/pdf") {
            val request = runCatching { call.receive<ExportRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid export request payload")
                )
            }

            if (request.title.isBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Document title must not be blank")
                )
            }

            val pdfBytes = runCatching { PdfExportWorker.generatePdf(request) }.getOrElse { error ->
                return@post call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to generate PDF: ${error.message}")
                )
            }

            val sanitizedFilename = request.title.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".pdf"
            call.response.header(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"$sanitizedFilename\""
            )
            call.respondBytes(pdfBytes, ContentType.Application.Pdf, HttpStatusCode.OK)
        }

        post("/export/docx") {
            val request = runCatching { call.receive<ExportRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid export request payload")
                )
            }

            if (request.title.isBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Document title must not be blank")
                )
            }

            val docxBytes = runCatching { DocxExportWorker.generateDocx(request) }.getOrElse { error ->
                return@post call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to generate DOCX: ${error.message}")
                )
            }

            val sanitizedFilename = request.title.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".docx"
            call.response.header(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"$sanitizedFilename\""
            )
            call.respondBytes(
                docxBytes,
                ContentType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                HttpStatusCode.OK
            )
        }

        post("/import/text") {
            val request = runCatching { call.receive<ImportRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid import request payload")
                )
            }

            if (request.rawContent.isBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("rawContent must not be blank")
                )
            }

            val response = ImportService.parseTextImport(request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
