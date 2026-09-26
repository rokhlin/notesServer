# Project Rules and Guidelines for AI Agent

This file contains foundational guidelines and project context for the backend service (`notesServer`).

## Language Requirements
- **All Markdown files (`*.md`) must strictly be written in English.** This applies to documentation, rules, skills, agents, and README files.

## Mandatory Task Initialization Rule
- **Inspect `docs/` First**: When a task is assigned or started, the agent MUST first inspect and read the relevant documentation files in the `docs/` folder to gather domain context, constraints, and architecture guidelines before planning or writing code.

## General Project Principles
- **Technology Stack**: Kotlin (Ktor Server / JVM).
- **Code Style**: Follow idiomatic Kotlin conventions and REST / WebSocket API best practices.
- **Data Models**: Data models must remain synchronized with client models.
