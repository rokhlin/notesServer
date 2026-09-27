# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Initial project structure for Ktor Server.
- Synchronized common domain models and DTOs (Note, InkStroke, CanvasLayer, CmnManifest, NoteMetadata, SyncDTOs) with client application.
- Routing, status pages, content negotiation, and CORS configuration.
- Implemented Ktor Auth Gateway with JWT access & refresh token rotation and BCrypt salted password hashing (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, and protected `GET /api/v1/auth/me`).
- Implemented Delta Sync protocol (`POST /api/v1/sync`) with timestamp watermark filtering, Last-Write-Wins (LWW) conflict resolution, and automatic revision history tracking (`GET /api/v1/notes/{id}/revisions`).
- Implemented Soft-Delete Recycle Bin (`GET /api/v1/trash`, `POST /api/v1/trash/{id}/restore`, `DELETE /api/v1/trash/{id}`, `DELETE /api/v1/trash`).
- Implemented Real-Time Collaboration WebSocket Gateway (`/api/v1/ws/notes/{noteId}`) with multi-client co-presence, operational text delta broadcasting, and single-editor exclusive canvas locking (ADR Q25).
- Implemented Headless Document Export Worker generating binary PDF documents via OpenPDF (`POST /api/v1/export/pdf`), Microsoft Word `.docx` documents via Apache POI (`POST /api/v1/export/docx`) with embedded canvas drawings, and Markdown text ingestion (`POST /api/v1/import/text`).
- GitHub Actions CI/CD workflows for PR validation, AI code review, and release automation.

## [0.0.1] - 2026-09-26
### Added
- Initial release of Notes Ktor Server.
