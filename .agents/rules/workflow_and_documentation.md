# Rule: Workflow, Documentation, and Issue Management

## 1. Documentation Structure
The root of the project must constantly maintain a `docs/` directory containing the following files:
- `Changelog.md` — Project modification history adhering to the "Keep a Changelog" standard.
- `features.md` — A checklist of planned and implemented new functionalities.
- `bugs.md` — A checklist of identified and resolved bugs.
- `system_architecture.md` — Comprehensive system architecture, API documentation, module descriptions, and flow diagrams.

## 2. Managing New Features (`features.md`)
- The file maintains a checklist format using Markdown (`- [ ] Feature Name`).
- **Upon receiving a task for new functionality:** The AI must first add a new entry to `features.md` if it does not already exist.
- **When instructed to implement a feature from the list:** The AI must read the feature description, analyze the context, and **output a step-by-step development plan** before writing any code.
- **Upon successful implementation, the AI must:**
  1. Mark the item as completed (`- [x] Feature Name`).
  2. Update `system_architecture.md` (sync diagrams, functions, and APIs with the new reality).
  3. Log the implemented feature in `Changelog.md` under the `[Unreleased]` section, categorized properly (e.g., `### Added` or `### Changed`).

## 3. Managing Bugs (`bugs.md`)
- The workflow mirrors feature development.
- Identified bugs must be logged in `bugs.md` as unchecked list items.
- When instructed to fix a bug, the AI must formulate a troubleshooting and resolution plan.
- After fixing, the AI must check off the item (`[x]`), adjust `system_architecture.md` if the fix required architectural shifts, and document the fix in `Changelog.md` under the `[Unreleased]` -> `### Fixed` section.

## 4. System Architecture & Diagramming Skill (`system_architecture.md`)
- This document must remain the single source of truth and be updated synchronously with any codebase changes (business logic, modules, APIs).
- **Diagramming Skill:** The AI must seamlessly generate and maintain architecture charts, sequence diagrams, and flowcharts directly inside the Markdown file using **Mermaid.js** syntax. No external image generation is required; rely exclusively on Mermaid blocks (````mermaid ````).

## 5. Release Cycle and Changelog Standard
- `Changelog.md` must strictly follow the **Keep a Changelog** standard, utilizing categories: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, and `Security`.
- **Release Automation:** The AI must develop and maintain a custom Gradle task (e.g., in `build.gradle.kts` or `buildSrc`) designed to be executed by GitHub Actions (e.g., `./gradlew generateRelease`).
- **On triggering the Release Task, the script must:**
  1. Parse the `[Unreleased]` block in `Changelog.md`.
  2. Prompt for or calculate the upcoming version number (SemVer).
  3. Replace the `[Unreleased]` header with the new version number and current date.
  4. Inject a new, empty `[Unreleased]` section at the top of the document.
  5. Export the parsed release notes to a format consumable by GitHub Actions for creating a GitHub Release.
