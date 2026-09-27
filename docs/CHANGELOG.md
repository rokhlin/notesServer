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
- GitHub Actions CI/CD workflows for PR validation, AI code review, and release automation.

## [0.0.1] - 2026-09-26
### Added
- Initial release of Notes Ktor Server.
