# Rule: System Architecture Documentation Standard

## 1. File Location and Purpose
The authoritative system architecture document must be maintained at `docs/system_architecture.md` in the root of the project. It serves as the single source of truth for the AI agent and human developers regarding the system's design and behavior. The document must be updated synchronously with any structural code changes.

## 2. Required Document Structure
The `system_architecture.md` file MUST contain the following sections:

### 2.1. Architectural Overview
- A detailed breakdown of the existing application architecture.
- Explicit separation between the Client-side architecture (KMP: Web, iOS, Android) and the Server-side architecture (Dockerized backend).

### 2.2. Flow Diagrams
- Visual representations of data flows, user journeys, and component interactions using **Mermaid.js** syntax exclusively.

### 2.3. API Contracts
- Comprehensive documentation of all REST and WebSocket APIs used for communication between the client and server.
- Each API entry must include:
  - Endpoint path and HTTP method.
  - Required Headers (e.g., Authorization tokens).
  - Query and Body Parameters (with types).
  - Concrete JSON examples of both Requests and Responses (for both Success and Error states).

### 2.4. Module and Function Definitions
- Detailed description of core functions, services, and business logic modules.
- Must describe expected behavior during normal execution (Happy Path).
- Must explicitly define behavior, state changes, and fallback mechanisms during failures or errors.

### 2.5. Security and Authorization
- An in-depth breakdown of the current security mechanisms.
- Details on User Authentication flows (e.g., session management, tokens).
- Details on the End-to-End (E2E) encryption pipeline for "Protected Notes," including local key derivation (e.g., Argon2), encryption algorithms, and secure storage constraints.

### 2.6. Feature Toggles
- Architectural description of the Feature Toggle (Feature Flags) system used for fine-grained system configuration.
- Explanation of how flags are fetched, evaluated, fallback defaults, and how they govern the activation of new code paths without deploying new versions.

### 2.7. Data Models & Schema (Recommended Addition)
- Entity-Relationship Diagrams (ERD) using Mermaid.js.
- Description of local (device) storage mechanisms vs. server-side database schemas.

### 2.8. Synchronization & Offline Strategy (Recommended Addition)
- Detailed explanation of the CRDT (Conflict-free Replicated Data Type) implementation for collaborative editing.
- State machine description for offline mode, queueing mechanisms for unsynced changes, and background synchronization intervals.

### 2.9. Deployment Architecture (Recommended Addition)
- Overview of the containerized Docker infrastructure, database connections, and CI/CD deployment targets.
- 