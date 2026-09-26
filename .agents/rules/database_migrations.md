# Rule: Database Schema Management and Migrations

## 1. Multi-Repository Scope
This rule applies across the distributed system architecture, which is strictly separated into independent GitHub repositories (e.g., a Server/Backend repository and a Client/KMP repository). When a new feature requires data persistence, the AI must evaluate and generate database migrations for **both** the server-side database and the client-side local databases, depending on the scope of the task.

## 2. Tooling Selection and Migration Strategy
- **Tooling:** The AI must select the most optimal, industry-standard migration tool suitable for the specific repository's tech stack (e.g., **Flyway**, **Liquibase**, or ORM-based migrations for the backend; **SQLDelight** or similar for the KMP client apps).
- **Forward-Only Migrations:** The AI must strictly generate "Up" migrations (scripts to advance the database schema). Automated "Down" (rollback) migration scripts must **not** be generated unless explicitly requested by the user.

## 3. Production & Live Device Safety Analysis
Before generating database-altering code, the AI must perform a **Live Data Impact Analysis**:
- **Server Database:** Ensure the schema change will not cause data loss, destructive locks, or unacceptable downtime in the production environment.
- **Client Databases:** Ensure local database migrations on mobile/web clients will seamlessly update the schema on user devices without corrupting or deleting existing offline/unsynced notes.
- The AI must explicitly plan strategies for handling `NULL` values, default values, and data mapping for existing rows.

## 4. Documentation & Architecture Sync
- Any change to the database schema (whether client or server) must be immediately documented in that specific repository's `system_architecture.md`.
- The AI must update or create Entity-Relationship diagrams using **Mermaid.js** (`erDiagram` syntax). The documentation must explicitly specify table names, column data types, Primary Keys (PK), Foreign Keys (FK), and relationships.