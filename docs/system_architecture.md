# System Architecture: Notes Alltogether Backend (`notesServer`)

## 1. Architectural Overview

`notesServer` is a high-throughput, containerized backend microservice built with **Kotlin** and **Ktor Server (JVM)**. It serves as the central synchronization engine, authentication authority, real-time collaboration hub, and headless document conversion worker for the Notes Alltogether ecosystem.

### 1.1. Core Capabilities
- **Authentication & Identity Gateway**: JWT access and refresh token management with BCrypt salted password hashing.
- **Delta Synchronization Engine**: Monotonic watermark timestamp filtering, Last-Write-Wins (LWW) conflict resolution, and automatic revision history tracking.
- **Real-Time Collaboration WebSocket Gateway**: Ephemeral co-presence broadcasting, operational text delta routing, and single-editor exclusive canvas locking.
- **Headless Document Export Worker**: Server-side conversion of markdown and vector notes into PDF (OpenPDF) and Microsoft Word DOCX (Apache POI).
- **Zero-Knowledge Blind Storage**: Strict privacy boundary where encrypted notes (`isEncrypted = true`) are treated as opaque binary blobs without server-side decryption capability.

### 1.2. Repository & Package Topography
```text
notesServer/
├── src/main/kotlin/com/notes/server/
│   ├── Application.kt           # Ktor server engine entrypoint & module configuration
│   ├── collab/                  # Real-time WebSocket session management & canvas lock arbitration
│   │   └── CollabRoomManager.kt
│   ├── export/                  # Headless PDF/DOCX exporters & text ingestion workers
│   │   ├── DocxExportWorker.kt
│   │   ├── ImportService.kt
│   │   └── PdfExportWorker.kt
│   ├── models/                  # Server-side entities, DTOs & database table bindings
│   ├── plugins/                 # Ktor plugins: HTTP routing, CORS, ContentNegotiation, StatusPages
│   ├── repository/              # Data persistence repositories (PostgreSQL / Exposed ORM)
│   │   ├── SyncRepository.kt
│   │   └── UserRepository.kt
│   ├── routes/                  # REST & WebSocket endpoint handlers
│   │   ├── AuthRoutes.kt
│   │   ├── CollabWebSocketRoutes.kt
│   │   ├── ExportRoutes.kt
│   │   ├── NotesRoutes.kt
│   │   └── SyncRoutes.kt
│   └── security/                # JWT token signing, verification & password hashing
│       ├── JwtTokenManager.kt
│       └── PasswordHasher.kt
└── docs/                        # Architectural specifications, changelog & dependencies
```

---

## 2. Flow Diagrams

### 2.1. Overall Server Architecture & Pipeline Diagram
```mermaid
graph TD
    Client["Client (notesClientApp)"] --> Gateway["Ktor HTTP / WebSocket Pipeline"]
    
    subgraph Ktor_Pipeline["Ktor Server Middleware"]
        CORS["CORS Plugin"]
        ContentNeg["ContentNegotiation (JSON)"]
        AuthFilter["JWT Authentication Verifier"]
        StatusErr["StatusPages Exception Handler"]
    end
    
    Gateway --> CORS
    CORS --> ContentNeg
    ContentNeg --> AuthFilter
    AuthFilter --> StatusErr
    
    subgraph Handlers["Route Handlers"]
        AuthR["AuthRoutes<br/>(/api/v1/auth/*)"]
        SyncR["SyncRoutes<br/>(/api/v1/sync)"]
        NotesR["NotesRoutes<br/>(/api/v1/notes/*, /api/v1/trash/*)"]
        CollabR["CollabWebSocketRoutes<br/>(/api/v1/ws/notes/*)"]
        ExportR["ExportRoutes<br/>(/api/v1/export/*)"]
    end
    
    StatusErr --> AuthR
    StatusErr --> SyncR
    StatusErr --> NotesR
    StatusErr --> CollabR
    StatusErr --> ExportR
    
    subgraph Services["Core Engines & Repositories"]
        UserRepo["UserRepository"]
        SyncRepo["SyncRepository"]
        CollabHub["CollabRoomManager"]
        ExportEng["Pdf & Docx Workers"]
    end
    
    AuthR --> UserRepo
    SyncR --> SyncRepo
    NotesR --> SyncRepo
    CollabR --> CollabHub
    ExportR --> ExportEng
    
    subgraph Infrastructure["Storage & External Infrastructure"]
        Postgres[("PostgreSQL 16 Database<br/>Users, Notes, Revisions, Trash")]
        ObjStore[("Pluggable Object Storage<br/>MinIO / Cloudflare R2 / AWS S3")]
    end
    
    UserRepo --> Postgres
    SyncRepo --> Postgres
    ExportEng --> ObjStore
```

### 2.2. Authentication & JWT Token Rotation Lifecycle
```mermaid
sequenceDiagram
    autonumber
    actor Client as notesClientApp
    participant Auth as AuthRoutes
    participant TokenMgr as JwtTokenManager
    participant DB as UserRepository (PostgreSQL)

    Client->>Auth: POST /api/v1/auth/login { username, password }
    Auth->>DB: findUserByUsername(username)
    DB-->>Auth: UserRecord(passwordHash, salt, id)
    Auth->>Auth: BCrypt.verify(password, passwordHash)
    alt Password Invalid
        Auth-->>Client: 401 Unauthorized { "error": "INVALID_CREDENTIALS" }
    else Password Valid
        Auth->>TokenMgr: generateAccessToken(userId, ttl = 1h)
        Auth->>TokenMgr: generateRefreshToken(userId, ttl = 30d)
        TokenMgr-->>Auth: Pair(accessToken, refreshToken)
        Auth->>DB: storeRefreshToken(userId, hashedRefreshToken)
        Auth-->>Client: 200 OK { accessToken, refreshToken, expiresIn: 3600 }
    end

    Note over Client,Auth: After 1 hour (Access token expires)
    Client->>Auth: POST /api/v1/auth/refresh { refreshToken }
    Auth->>TokenMgr: verifyRefreshToken(refreshToken)
    Auth->>DB: validateStoredToken(userId, refreshToken)
    Auth->>TokenMgr: generateAccessToken(userId)
    Auth-->>Client: 200 OK { accessToken, expiresIn: 3600 }
```

### 2.3. Delta Synchronization & LWW Conflict Resolution Flow
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client App
    participant Sync as SyncRoutes
    participant Repo as SyncRepository
    participant DB as PostgreSQL (Notes & Revisions)

    Client->>Sync: POST /api/v1/sync { lastSyncTimestamp, clientChanges[] }
    Sync->>Repo: processSync(userId, lastSyncTimestamp, clientChanges)
    loop For each incoming client Note
        Repo->>DB: fetchExistingNote(note.id, userId)
        alt Note does not exist
            Repo->>DB: insertNewNote(note)
        else Server note exists & note.updatedAt > server.updatedAt
            Repo->>DB: archiveToRevisionHistory(serverNote)
            Repo->>DB: updateNotePayload(note)
        else Server note is newer (server.updatedAt >= note.updatedAt)
            Repo->>Repo: Keep server version (LWW resolution)
        end
    end
    Repo->>DB: queryNotesModifiedAfter(userId, lastSyncTimestamp)
    DB-->>Repo: serverModifiedNotes[]
    Repo-->>Sync: SyncResult(syncWatermark = now(), serverChanges)
    Sync-->>Client: 200 OK { syncWatermark, serverChanges, conflictsResolved }
```

---

## 3. API Contracts (Complete Specifications)

### 3.1. Authentication APIs

#### `POST /api/v1/auth/register`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "alice",
    "email": "alice@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Success Response (201 Created)**:
  ```json
  {
    "userId": "usr_94a8f1b2",
    "username": "alice",
    "createdAt": 1727438400000
  }
  ```
- **Error Response (409 Conflict)**:
  ```json
  {
    "error": "USERNAME_ALREADY_EXISTS",
    "message": "User with username alice already exists"
  }
  ```

#### `POST /api/v1/auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "alice",
    "password": "StrongPassword123!"
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...",
    "userId": "usr_94a8f1b2",
    "expiresIn": 3600
  }
  ```
- **Error Response (401 Unauthorized)**:
  ```json
  {
    "error": "INVALID_CREDENTIALS",
    "message": "Invalid username or password"
  }
  ```

#### `GET /api/v1/auth/me`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
  ```json
  {
    "userId": "usr_94a8f1b2",
    "username": "alice",
    "email": "alice@example.com",
    "createdAt": 1727438400000
  }
  ```
- **Error Response (401 Unauthorized)**:
  ```json
  {
    "error": "UNAUTHORIZED",
    "message": "Missing or expired Bearer token"
  }
  ```

### 3.2. Delta Synchronization & Revision APIs

#### `POST /api/v1/sync`
- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "lastSyncTimestamp": 1727438400000,
    "clientChanges": [
      {
        "id": "note_01HXYZ",
        "title": "Architecture Blueprint",
        "noteType": "MARKDOWN",
        "isEncrypted": false,
        "isDeleted": false,
        "updatedAt": 1727439100000,
        "payload": "# High Level System Design\n..."
      }
    ]
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "syncWatermark": 1727439150000,
    "serverChanges": [
      {
        "id": "note_02MNO",
        "title": "Server Deployment",
        "noteType": "MARKDOWN",
        "isEncrypted": false,
        "isDeleted": false,
        "updatedAt": 1727439120000,
        "payload": "Dockerized container on Port 8080"
      }
    ],
    "conflictsResolved": []
  }
  ```
- **Error Response (400 Bad Request)**:
  ```json
  {
    "error": "INVALID_SYNC_PAYLOAD",
    "message": "Malformed note change payload"
  }
  ```

#### `GET /api/v1/notes/{id}/revisions`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Parameters**: `id` (path, string)
- **Success Response (200 OK)**:
  ```json
  {
    "noteId": "note_01HXYZ",
    "revisions": [
      {
        "revisionId": "rev_01A",
        "updatedAt": 1727438000000,
        "title": "Architecture Blueprint (Draft)",
        "payload": "# Draft Architecture\n..."
      }
    ]
  }
  ```

### 3.3. Recycle Bin / Trash Lifecycle APIs

#### `GET /api/v1/trash`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
  ```json
  {
    "trashNotes": [
      {
        "id": "note_03OLD",
        "title": "Deprecated Spec",
        "deletedAt": 1727435000000
      }
    ]
  }
  ```

#### `POST /api/v1/trash/{id}/restore`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
  ```json
  {
    "id": "note_03OLD",
    "restored": true,
    "updatedAt": 1727439300000
  }
  ```

#### `DELETE /api/v1/trash/{id}`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Success Response (200 OK)**:
  ```json
  {
    "id": "note_03OLD",
    "purged": true
  }
  ```

### 3.4. Real-Time Collaboration WebSocket Gateway

#### `WS /api/v1/ws/notes/{noteId}`
- **Query Parameter**: `token=<accessToken>`
- **Incoming Messages**:
  - `{"type": "CANVAS_LOCK_REQUEST", "layerId": "layer_01"}`
  - `{"type": "TEXT_OP", "delta": {"retain": 10, "insert": "Hello "}}`
  - `{"type": "CURSOR_MOVE", "x": 120.5, "y": 450.0}`
- **Outgoing Broadcasts**:
  - `{"type": "PRESENCE", "activeUsers": [{"userId": "usr_94a8f1b2", "color": "#4F46E5"}]}`
  - `{"type": "CANVAS_LOCK_GRANTED", "layerId": "layer_01", "lockedBy": "usr_94a8f1b2"}`
  - `{"type": "CANVAS_LOCK_DENIED", "layerId": "layer_01", "lockedBy": "usr_other"}`

### 3.5. Document Export & Import APIs

#### `POST /api/v1/export/pdf`
- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "noteId": "note_01HXYZ",
    "title": "Architecture Blueprint",
    "contentMarkdown": "# System Design\n...",
    "svgVectorDrawings": ["<svg xmlns=...>...</svg>"]
  }
  ```
- **Success Response (200 OK)**:
  - **Headers**: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="Architecture_Blueprint.pdf"`
  - **Body**: Binary PDF byte stream.

#### `POST /api/v1/export/docx`
- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Request Body**: Identical to PDF export.
- **Success Response (200 OK)**:
  - **Headers**: `Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `Content-Disposition: attachment; filename="Architecture_Blueprint.docx"`
  - **Body**: Binary DOCX byte stream.

---

## 4. Module & Function Definitions

### 4.1. Security & Tokens (`com.notes.server.security`)
- **`JwtTokenManager`**:
  - `generateAccessToken(userId: String): String`: Signs HMAC-SHA256 JWT with 1-hour expiration and subject `userId`.
  - `generateRefreshToken(userId: String): String`: Generates cryptographically secure 256-bit random token.
  - `verifyToken(token: String): DecodedJWT?`: Validates token signature, expiration, and issuer. Returns null on expired or tampered token.
- **`PasswordHasher`**:
  - `hash(password: String): String`: Generates BCrypt hash with 12 salt rounds.
  - `verify(password: String, hash: String): Boolean`: Constant-time verification preventing timing attacks.

### 4.2. Synchronization Engine (`com.notes.server.repository.SyncRepository`)
- **`processSync(userId: String, watermark: Long, changes: List<NoteSyncDTO>): SyncResponse`**:
  - Inspects incoming client modifications against server PostgreSQL database.
  - Applies LWW rule: if incoming note has strictly higher `updatedAt`, archives current database note to `NoteRevisionsTable` and overwrites active note.
  - Queries all notes for `userId` modified after `watermark` and returns them as `serverChanges`.
  - *Error Fallback*: Catches database concurrency exceptions and rolls back transaction cleanly.

### 4.3. Real-Time Collaboration Hub (`com.notes.server.collab.CollabRoomManager`)
- **`joinRoom(noteId: String, session: CollabSession)`**:
  - Binds client WebSocket session to room channel; broadcasts updated user list.
- **`requestCanvasLock(noteId: String, layerId: String, userId: String): Boolean`**:
  - Grants exclusive editing lock for vector layer if currently unreserved or owned by the same user.
  - Auto-releases lock after inactivity timeout (default 30 seconds).

### 4.4. Headless Exporters (`com.notes.server.export`)
- **`PdfExportWorker`**:
  - `generatePdf(title: String, markdown: String, svgs: List<String>): ByteArray`
  - Utilizes OpenPDF to render typography, table structures, headings, and converts SVG vectors into embedded high-DPI graphics.
- **`DocxExportWorker`**:
  - `generateDocx(title: String, markdown: String, svgs: List<String>): ByteArray`
  - Uses Apache POI to assemble standard OpenXML document paragraphs, bullet lists, and vector graphics.

---

## 5. Security & Authorization Architecture

### 5.1. Authentication Architecture
- **JWT Standard**: Stateless access tokens signed with a 512-bit secret key (`JWT_SECRET`).
- **Token Expiry**:
  - Short-lived Access Token: 60 minutes.
  - Long-lived Refresh Token: 30 days with revocation on sign-out or password change.
- **Password Protection**: BCrypt algorithm with 12 salt rounds.

### 5.2. Zero-Knowledge Blind Data Store
- Protected notes marked with `isEncrypted = true` store encrypted payloads directly in `payload`.
- The server does NOT possess user encryption keys and performs zero introspection of encrypted payloads.
- Server provides encrypted blob synchronization without violating end-to-end privacy guarantees.

---

## 6. Feature Toggles & Configuration

Configuration is managed declaratively via environment variables and `application.conf`:

| Variable | Description | Default | Governing Component |
| :--- | :--- | :--- | :--- |
| `PORT` | HTTP/WS listen port | `8080` | `Application.kt` |
| `JWT_SECRET` | Secret key for JWT signing | *Required* | `JwtTokenManager` |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/notes` | `DatabaseFactory` |
| `STORAGE_PROVIDER` | Object storage engine (`S3`, `R2`, `MINIO`, `LOCAL`) | `MINIO` | Object Storage Gateway |
| `CANVAS_LOCK_TTL` | Inactivity lock timeout (seconds) | `30` | `CollabRoomManager` |
| `REVISION_LIMIT` | Max revisions stored per note | `20` | `SyncRepository` |

---

## 7. Data Models & Database Schemas

### 7.1. Database Schema Diagram (PostgreSQL / Exposed ORM)
```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        bigint created_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        bigint expires_at
    }

    NOTES {
        varchar id PK
        uuid user_id FK
        varchar title
        varchar note_type
        boolean is_encrypted
        boolean is_deleted
        text payload
        bigint created_at
        bigint updated_at
    }

    NOTE_REVISIONS {
        uuid revision_id PK
        varchar note_id FK
        uuid user_id FK
        varchar title
        text payload
        bigint archived_at
    }

    USERS ||--o{ REFRESH_TOKENS : "owns"
    USERS ||--o{ NOTES : "creates"
    NOTES ||--o{ NOTE_REVISIONS : "has history"
```

### 7.2. Exposed Table Definitions
```kotlin
object NotesTable : Table("notes") {
    val id = varchar("id", 64)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val noteType = varchar("note_type", 32)
    val isEncrypted = bool("is_encrypted").default(false)
    val isDeleted = bool("is_deleted").default(false)
    val payload = text("payload")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
```

---

## 8. Synchronization & Concurrency Strategy

1. **Watermark Delta Sync**: Efficient synchronization transmits only delta changes using UTC epoch milliseconds.
2. **Conflict Resolution Policy**:
   - Monotonic timestamps govern resolution via Last-Write-Wins (LWW).
   - Whenever an update overwrites an existing note version, the superseded content is automatically written to `note_revisions`, ensuring zero accidental data loss.
3. **Canvas Locking Concurrency**:
   - Real-time handwritten drawing enforces single-writer locking per canvas layer to prevent incompatible Bézier curve merging artifacts.
   - Locks automatically expire after 30 seconds of inactivity if not refreshed with a heartbeat ping.

---

## 9. Deployment Architecture

```mermaid
graph TD
    Client["Client Traffic (HTTPS / WSS)"] --> Ingress["Reverse Proxy (Nginx / Cloudflare)"]
    Ingress --> App["Ktor Server Container (Port 8080)<br/>Alpine JRE 21"]
    
    subgraph Docker_Compose["Dockerized Infrastructure"]
        App --> Postgres[("PostgreSQL 16 Service<br/>Port 5432")]
        App --> Storage[("MinIO Object Storage<br/>Port 9000")]
    end

    subgraph CI_CD["Automated GitHub Actions Pipeline"]
        PR_Val["pr_validation.yml<br/>(Gradle check, Tests >= 75%, Linters)"]
        Docker_Build["Docker Image Build & Push"]
        Release["release.yml<br/>(Automated Semantic Tagging)"]
    end

    App --> CI_CD
```

- **Containerization**: Multi-stage `Dockerfile` creating a lightweight, secure image based on `eclipse-temurin:21-jre-alpine`.
- **Local Dev Orchestration**: `docker-compose.yml` pre-configures PostgreSQL and MinIO for immediate local testing.
- **Health Probes**: Liveness and readiness endpoints available at `GET /health`.
