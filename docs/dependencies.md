# System Dependencies & Technology Stack Matrix

**Document Version:** 1.1.0  
**Status:** Living Document (Updated following architectural alignment & 30/30 Q&A resolution)  
**Target Projects:** `notesClientApp` (Kotlin Multiplatform / Compose Multiplatform) & `notesServer` (Kotlin / Ktor Server)  
**Governing Specs:** [features.md](file:///c:/projects/NotesAlltogether/notesServer/docs/features.md)

---

## 1. Overview & Categorization Strategy

This document tracks all active, planned, candidate, and rejected dependencies required to implement the features defined in `features.md`, `ui_ux_text_editor.md`, and `ui_ux_handwritten_notes.md`.

Dependencies are organized across architectural domains:
1. **Core Runtime & UI Framework**
2. **Text Editor Module (Pluggable Markdown Engines)**
3. **Handwritten Canvas Module (Skia Graphics, Splines & SVG Export)**
4. **Local Storage, File System & Packaging (`.cmn`, `TextBundle`, JSON Index)**
5. **Security, End-to-End Encryption (E2EE) & Authentication**
6. **Networking, Delta Synchronization & Settings Persistence**
7. **Real-Time Collaboration & Concurrency Engine (WebSockets)**
8. **Document Import, Export & Rendering Engine (Server-side PDF & DOCX)**
9. **Server Backend, Database & Object Storage (Ktor JVM)**
10. **Development Tooling, Design System & AI Customizations**

---

## 2. Master Dependencies Table

| Category | Target | Dependency / Library | Gradle / Maven Coordinates | Purpose & Feature Mapping | Status | Decision Context / Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Core UI** | Client | **Compose Multiplatform UI** | `org.jetbrains.compose.ui:ui` | Multiplatform declarative UI foundation | **Active** | Core UI runtime |
| **Core UI** | Client | **Material 3 Multiplatform** | `org.jetbrains.compose.material3:material3` | Adaptive themes, color tokens, and components | **Active** | Adopted as primary UI design system (Q30) |
| **Core UI** | Client | **Compose Navigation** | `org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10` | Type-safe multiplatform navigation routing | **Planned (Phase 0)** | Chosen navigation framework (Q3) |
| **Core UI** | Client | **Window Size Class** | `org.jetbrains.compose.material3:material3-window-size-class` | Responsive layout switching (Phone vs Foldable vs Tablet) | **Planned (Phase 0)** | Adaptive drawer vs multi-pane UI |
| **Editor** | Client | **Markdown Parser (AST)** | `org.jetbrains.markdown:markdown:0.7.3` | CommonMark AST parser for Source and Reading modes | **Planned (Phase 1)** | Core parser for default engine |
| **Editor** | Client | **Multiplatform Markdown Renderer** | `com.mikepenz:multiplatform-markdown-renderer:0.16.0` | Live Preview and Reading Mode inline rendering | **Planned (Phase 1)** | **Default** WYSIWYG plugin engine (Q5) |
| **Editor** | Client | **Compose RichText** | `com.halilibo.compose-richtext:richtext-commonmark:0.17.0` | Alternative rich text editing engine | **Planned (Phase 1)** | **Secondary** pluggable engine selectable in settings (Q5) |
| **Canvas** | Client | **Skia Graphics Multiplatform** | Bundled via `compose.ui:ui-graphics` | High-performance hardware-accelerated 2D canvas | **Active** | Core drawing engine |
| **Canvas** | Client | **Spline Interpolation Utility** | In-house math package (`common-models/math`) | Catmull-Rom smoothing for fluid vector ink strokes | **Planned (Phase 2)** | Chosen spline smoothing model (Q10) |
| **Canvas** | Client | **SVG Export Generator** | In-house / `org.jetbrains.compose.ui:ui-graphics` | Exporting vector strokes from `.cmn` container to SVG | **Planned (Phase 2)** | Added per user requirement (Q11) |
| **Storage** | Client | **Lightweight JSON Index** | `kotlinx.serialization.json` + `okio` | Filesystem-based JSON catalog for notes & tags | **Planned (Phase 3)** | Chosen over SQL DB for simplicity & speed (Q19) |
| **Storage** | Client | **Room Multiplatform** | `androidx.room:room-runtime:2.7.0-alpha10` | Embedded SQLite database | **Rejected** | Replaced by lightweight JSON file index (Q19) |
| **Storage** | Client | **SQLDelight** | `app.cash.sqldelight:runtime:2.0.2` | Type-safe multiplatform database engine | **Rejected** | Replaced by lightweight JSON file index (Q19) |
| **Storage** | Client | **Okio Multiplatform** | `com.squareup.okio:okio:3.9.1` | Cross-platform file I/O, streaming, and hashing | **Planned (Phase 3)** | Sandboxed file storage & ZIP handling |
| **Storage** | Client | **DataStore Preferences** | `androidx.datastore:datastore-preferences-core:1.1.1` | App settings and action toolbar configuration | **Planned (Phase 1)** | Chosen for toolbar & sync settings (Q7, Q20) |
| **Packaging** | Client/Common | **Compress-Zip KMP** | In-house Okio Zip Stream | Non-destructive `.cmn` and `TextBundle` packaging | **Planned (Phase 2)** | Custom magic byte header & ZIP container |
| **Security** | Client | **Argon2 KMP** | `de.charlex.compose:argon2-kmp:1.0.0` or native C/JNI | Passphrase key derivation for zero-knowledge vault | **Planned (Phase 3)** | Key derivation for E2EE protected notes |
| **Security** | Client | **Crypto KMP (AES-GCM)** | `org.kotlincrypto.cipher:aes-gcm:0.3.0` | Authenticated AES-GCM-256 for protected notes | **Planned (Phase 3)** | Encrypts note body & attachments (Q17) |
| **Security** | Client | **BIP-39 Mnemonic Recovery** | In-house BIP-39 wordlist generator | 12-word seed phrase emergency recovery kit | **Planned (Phase 3)** | Recovery kit on vault setup (Q15) |
| **Security** | Client | **Biometric KMP** | `dev.icerock.moko:biometry:0.4.0` | Biometric fingerprint/Face ID unlock adapter | **Planned (Phase 3)** | Hardware Keystore/Keychain unlocker (Q16) |
| **Security** | Server | **Ktor Auth JWT** | `io.ktor:ktor-server-auth-jwt:3.0.1` | Token-based user authentication and claims verification | **Planned (Phase 4)** | Email/Password + OAuth2 architecture (Q18) |
| **Security** | Server | **BCrypt JVM** | `org.mindrot:jbcrypt:0.4` | Secure server password hashing | **Planned (Phase 4)** | Account credential hashing |
| **Networking**| Client | **Ktor Client Core** | `io.ktor:ktor-client-core:3.0.1` | Multiplatform HTTP REST engine | **Planned (Phase 4)** | Delta sync & attachment uploads (max 25MB, Q22) |
| **Networking**| Client | **Ktor Client Engines** | `io.ktor:ktor-client-okhttp` (Android), `darwin` (iOS) | Platform-specific high-efficiency HTTP transports | **Planned (Phase 4)** | Native network engines |
| **Networking**| Client | **Ktor Client WebSockets** | `io.ktor:ktor-client-websockets:3.0.1` | Real-time bi-directional collaborative streaming | **Planned (Phase 5)** | WebSocket client for text collab |
| **Collab** | Client/Server | **Ktor WebSockets (Server)**| `io.ktor:ktor-server-websockets:3.0.1` | Room-based concurrent editing event hub | **Planned (Phase 5)** | Server-authoritative delta broadcast (Q24) |
| **Collab** | Client/Common | **Heavy CRDT (Yjs/Automerge)**| N/A | Complex peer-to-peer character-level CRDT | **Rejected (MVP)** | Using server-authoritative delta streaming (Q24) |
| **Export** | Server | **OpenPDF JVM** | `com.github.librepdf:openpdf:2.0.3` | Server-side headless worker for pixel-perfect PDF | **Planned (Phase 6)** | Delegated server PDF worker (Q27) |
| **Export** | Server | **Apache POI (docx)** | `org.apache.poi:poi-ooxml:5.3.0` | Generating formatted Word documents (.docx) | **Planned (Phase 6)** | Formatted text + rasterized ink snapshots (Q28) |
| **Database** | Server | **Exposed ORM** | `org.jetbrains.exposed:exposed-core:0.56.0` | Idiomatic Kotlin SQL ORM | **Planned (Phase 4)** | User accounts, metadata, and version history |
| **Database** | Server | **PostgreSQL JDBC** | `org.postgresql:postgresql:42.7.4` | Production relational database driver | **Planned (Phase 4)** | Server database backend |
| **Database** | Server | **HikariCP** | `com.zaxxer:HikariCP:6.0.0` | High-performance JDBC connection pooling | **Planned (Phase 4)** | Connection pool |
| **Database** | Server | **Flyway Migrations** | `org.flywaydb:flyway-core:10.20.1` | Automated database schema evolution | **Planned (Phase 4)** | DB migrations |
| **Object Storage**| Server | **AWS S3 / MinIO SDK** | `aws.sdk.kotlin:s3:1.3.62` | Cloud object storage for large attachments & backups | **Planned (Phase 4)** | Storage for media attachments (up to 25MB) |

---

## 3. Tooling, MCP & Agent Integrations

| Tool / Resource | Integration Scope | Purpose | Status | Decision Context / Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **Figma MCP Server** | Antigravity IDE | Sync design tokens & layouts from Figma | **Postponed / Not Applicable** | No Figma link available; developing with Material 3 in code (Q30) |
| **Android Virtual Devices** | `android-cli-plugin` | UI inspection, screenshots, and foldable display emulation | **Available** | Primary testing ground for Android tablet/foldable targets |
| **DevTools MCP** | `chrome-devtools-plugin` | Performance profiling, memory leak detection, Wasm debugging | **Available** | Web/Wasm target verification |
| **Subagents** (`.agents/agents`) | Project-level AI Roles | Specialized agents for Canvas, Markdown Editor, Crypto Vault, and Sync | **Planned** | Roles to be created for phased tasks |
| **Custom Skills** (`.agents/skills`) | Knowledge Guides | Procedural cheatsheets for Skia Canvas drawing and `.cmn` file format | **Planned** | Multiplatform Canvas & container format guides |

---

## 4. Architectural Decisions Register (from Clarifying Q&A)

1. **Target Platforms (Q1, Q4):** Primary: Android (API 28+, tablet/foldable/phone), followed by Desktop (JVM 17/21) and iOS (16.0+).
2. **Shared Code (Q2):** Common Kotlin Multiplatform module `:common-models` to be created.
3. **Navigation (Q3):** Jetpack Navigation Compose Multiplatform.
4. **Editor Engines (Q5):** Pluggable architecture. Engine 1 (`multiplatform-markdown-renderer`) as default; Engine 2 (`compose-richtext`) as secondary option in settings.
5. **UI Navigation (Q6):** No tabs on any device; clean sidebar and modal quick switcher.
6. **Tags (Q8):** Hierarchical nested tags (`#work/project1/urgent`) with tree view in sidebar.
7. **Wikilinks (Q9):** Dual support for standard Markdown `[text](url)` and Obsidian `[[Note Name]]` with auto-complete.
8. **Drawing Smoothing & Vectors (Q10, Q11):** Catmull-Rom spline interpolation; JSON stroke storage in `.cmn`; SVG export support.
9. **Stylus Hardware Support (Q12):** Dynamic pressure and tilt thickness on S-Pen and Apple Pencil.
10. **Shape Recognition (Q13):** Dual: toolbar shape tools + auto-snapping on draw-and-hold (0.5s).
11. **Canvas Roll (Q14):** Vertically continuous page roll with page break dividers.
12. **E2EE Recovery & Indexing (Q15, Q16, Q17):** Zero-Knowledge with 12-word seed phrase; optional biometrics (Keystore/Keychain); Title-only search (titles unencrypted metadata, bodies/drawings/media strictly encrypted).
13. **Local Database (Q19):** Custom lightweight JSON index files on the filesystem; SQL databases (Room/SQLDelight) rejected for local client storage.
14. **Sync & Deletion (Q20, Q21, Q22, Q23):** Hybrid sync (debounce + backgrounding + manual + settings-configured periodic timer); LWW conflict resolution with server revision history; Soft deletion with a Recycle Bin and manual emptying; 25 MB max attachment limit; Full local caching by default with on-demand toggle in settings.
15. **Collaboration Scope (Q24, Q25, Q26):** Server-authoritative WebSocket delta broadcast for text; single-editor exclusive lock for handwritten notes; registered accounts only (external sharing via common file export).
16. **Export & Storage (Q27, Q28, Q29):** Headless server-side PDF worker; `.docx` export includes formatted text and rasterized canvas pages; sandboxed app storage.
17. **Design Workflow (Q30):** Material 3 code-first UI without external Figma dependency.
