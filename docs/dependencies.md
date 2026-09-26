# System Dependencies & Technology Stack Matrix

**Document Version:** 1.0.0  
**Status:** Living Document (Updated per feature analysis and implementation stage)  
**Target Projects:** `notesClientApp` (Kotlin Multiplatform / Compose Multiplatform) & `notesServer` (Kotlin / Ktor Server)  
**Governing Specs:** [features.md](file:///c:/projects/NotesAlltogether/notesServer/docs/features.md)

---

## 1. Overview & Categorization Strategy

This document tracks all active, planned, and candidate dependencies required to implement the features defined in `features.md`, `ui_ux_text_editor.md`, and `ui_ux_handwritten_notes.md`. 

Dependencies are organized across the project lifecycle and architectural domains:
1. **Core Runtime & UI Framework**
2. **Text Editor Module (Obsidian UX & Markdown Engine)**
3. **Handwritten Canvas Module (Samsung Notes UX & Skia Graphics)**
4. **Local Storage, File System & Compound Packaging (`.cmn`, `TextBundle`)**
5. **Security, End-to-End Encryption (E2EE) & Authentication**
6. **Networking, Delta Synchronization & Background Workers**
7. **Real-Time Collaboration & Concurrency Engine (WebSockets & CRDT)**
8. **Document Import, Export & Rendering Engine (PDF, DOCX, Images)**
9. **Server Backend, Database & Object Storage (Ktor JVM)**
10. **Development Tooling, Design Systems (Figma MCP) & AI Customizations**

---

## 2. Master Dependencies Table

| Category | Target | Dependency / Library | Gradle / Maven Coordinates | Purpose & Feature Mapping | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Core UI** | Client | **Compose Multiplatform UI** | `org.jetbrains.compose.ui:ui` | Multiplatform declarative UI foundation | **Active** |
| **Core UI** | Client | **Material 3 Multiplatform** | `org.jetbrains.compose.material3:material3` | Adaptive themes, color system, and UI components | **Active** |
| **Core UI** | Client | **Compose Navigation** | `org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10` | Type-safe multiplatform navigation routing | Planned (Phase 0) |
| **Core UI** | Client | **Window Size Class** | `org.jetbrains.compose.material3:material3-window-size-class` | Responsive layout switching (Phone vs Foldable vs Tablet) | Planned (Phase 0) |
| **Editor** | Client | **Markdown Parser (AST)** | `org.jetbrains.markdown:markdown:0.7.3` | CommonMark AST parser for Source and Reading modes | Planned (Phase 1) |
| **Editor** | Client | **Multiplatform Markdown Renderer** | `com.mikepenz:multiplatform-markdown-renderer:0.16.0` | Live Preview and Reading Mode inline rendering | Planned (Phase 1) |
| **Editor** | Client | **Compose RichText** | `com.halilibo.compose-richtext:richtext-commonmark:0.17.0` | Rich text editing primitives and annotated string conversion | Candidate (Phase 1) |
| **Canvas** | Client | **Skia Graphics Multiplatform** | Bundled via `compose.ui:ui-graphics` | High-performance hardware-accelerated 2D canvas | **Active** |
| **Canvas** | Client | **Spline Interpolation Utility** | In-house math package (`common-models/math`) | Catmull-Rom smoothing for fluid vector ink strokes | Planned (Phase 2) |
| **Storage** | Client | **Room Multiplatform** | `androidx.room:room-runtime:2.7.0-alpha10` | Offline-first SQLite database for note metadata and search | Planned (Phase 3) |
| **Storage** | Client | **SQLDelight** (Alternative) | `app.cash.sqldelight:runtime:2.0.2` | Type-safe multiplatform database engine | Candidate (Phase 3) |
| **Storage** | Client | **Okio Multiplatform** | `com.squareup.okio:okio:3.9.1` | Cross-platform file I/O, streaming, and hashing | Planned (Phase 3) |
| **Packaging** | Client/Common | **Compress-Zip KMP** | `com.eygraber:concurrency-kmp` / In-house Okio Zip | Non-destructive `.cmn` and `TextBundle` packaging | Planned (Phase 2) |
| **Security** | Client | **Argon2 KMP** | `de.charlex.compose:argon2-kmp:1.0.0` or native C/JNI | Passphrase key derivation for zero-knowledge vault | Planned (Phase 3) |
| **Security** | Client | **Crypto KMP (AES-GCM)** | `org.kotlincrypto.cipher:aes-gcm:0.3.0` | Authenticated AES-GCM-256 for protected notes | Planned (Phase 3) |
| **Security** | Client | **Biometric KMP** | `dev.icerock.moko:biometry:0.4.0` | Biometric fingerprint/Face ID unlock adapter | Planned (Phase 3) |
| **Security** | Server | **Ktor Auth JWT** | `io.ktor:ktor-server-auth-jwt:3.0.1` | Token-based user authentication and claims verification | Planned (Phase 4) |
| **Security** | Server | **BCrypt / Argon2 JVM** | `org.mindrot:jbcrypt:0.4` or `de.mkammerer:argon2-jvm:2.11` | Secure server password hashing | Planned (Phase 4) |
| **Networking**| Client | **Ktor Client Core** | `io.ktor:ktor-client-core:3.0.1` | Multiplatform HTTP REST engine | Planned (Phase 4) |
| **Networking**| Client | **Ktor Client Engines** | `io.ktor:ktor-client-okhttp` (Android), `darwin` (iOS) | Platform-specific high-efficiency HTTP transports | Planned (Phase 4) |
| **Networking**| Client | **Ktor Client WebSockets** | `io.ktor:ktor-client-websockets:3.0.1` | Real-time bi-directional collaborative streaming | Planned (Phase 5) |
| **Collab** | Client/Server | **Ktor WebSockets (Server)**| `io.ktor:ktor-server-websockets:3.0.1` | Room-based concurrent editing event hub | Planned (Phase 5) |
| **Collab** | Client/Common | **CRDT / Operational Sync** | `net.folivo:trixnity` or custom Delta CRDT | Real-time conflict-free concurrent editing | Candidate (Phase 5) |
| **Export** | Client/Server | **OpenPDF / iText JVM** | `com.github.librepdf:openpdf:2.0.3` | High-fidelity server-side or desktop PDF generation | Planned (Phase 6) |
| **Export** | Server | **Apache POI (docx)** | `org.apache.poi:poi-ooxml:5.3.0` | Ingestion and export of Microsoft Word (`.docx`) | Planned (Phase 6) |
| **Database** | Server | **Exposed ORM** | `org.jetbrains.exposed:exposed-core:0.56.0` | Idiomatic Kotlin SQL ORM | Planned (Phase 4) |
| **Database** | Server | **PostgreSQL JDBC** | `org.postgresql:postgresql:42.7.4` | Production relational database driver | Planned (Phase 4) |
| **Database** | Server | **HikariCP** | `com.zaxxer:HikariCP:6.0.0` | High-performance JDBC connection pooling | Planned (Phase 4) |
| **Database** | Server | **Flyway Migrations** | `org.flywaydb:flyway-core:10.20.1` | Automated database schema evolution | Planned (Phase 4) |
| **Object Storage**| Server | **AWS S3 / MinIO SDK** | `aws.sdk.kotlin:s3:1.3.62` | Cloud object storage for large attachments & backups | Planned (Phase 4) |

---

## 3. Tooling, MCP & Agent Integrations

| Tool / Resource | Integration Scope | Purpose | Status |
| :--- | :--- | :--- | :--- |
| **Figma MCP Server** | Antigravity IDE | Direct sync of design tokens, color palette, icons, and responsive layouts | **Ready to Connect** (Awaiting Figma Token/URL) |
| **Android Virtual Devices** | `android-cli-plugin` | UI inspection, screenshots, and foldable display emulation | **Available** |
| **DevTools MCP** | `chrome-devtools-plugin` | Performance profiling, memory leak detection, Wasm debugging | **Available** |
| **Subagents** (`.agents/agents`) | Project-level AI Roles | Specialized agents for Canvas, Markdown Editor, Crypto Vault, and Sync | Planned |
| **Custom Skills** (`.agents/skills`) | Knowledge Guides | Procedural cheatsheets for Skia Canvas drawing and `.cmn` file format | Planned |

---

## 4. Feature-to-Dependency Mapping Rules

When adding a new feature during analysis:
1. **Multiplatform First:** Prefer Kotlin Multiplatform (`commonMain`) libraries that support Android, iOS, Desktop, and WasmJs targets.
2. **Version Catalog Alignment:** All new client dependencies must be registered in [notesClientApp/gradle/libs.versions.toml](file:///c:/projects/NotesAlltogether/notesClientApp/gradle/libs.versions.toml).
3. **Server Catalog Alignment:** All new server dependencies must be registered in [notesServer/gradle/libs.versions.toml](file:///c:/projects/NotesAlltogether/notesServer/gradle/libs.versions.toml).
4. **Minimal Binary Footprint:** For zero-knowledge E2EE and custom `.cmn` containers, prefer pure-Kotlin or lightweight cryptographic implementations without heavy native bindings.
