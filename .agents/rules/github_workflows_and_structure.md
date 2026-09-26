# Rule: Repository Structure and CI/CD Automation

## 1. Multi-Repository Structure
The system must be strictly divided into separate GitHub projects:
- **Server Repository:** Contains the backend application, Docker configuration, and server-specific logic.
- **Client Repository (Kotlin Multiplatform):** Contains the shared cross-platform logic and the specific UI implementations for Web, iOS, and Android within a single unified project workspace.

## 2. Pull Request Automation (GitHub Actions)
For every Pull Request created in either repository, the AI must ensure the presence and maintenance of a CI pipeline (`.github/workflows/pr_validation.yml`) that executes the following jobs:
1. **Validation & Testing:** 
   - Compile the project to ensure no build errors exist.
   - Run all existing unit and integration tests.
   - The PR must fail if compilation or tests fail.
2. **Security Vulnerability Scan:**
   - Execute a dependency scanning tool (e.g., GitHub Dependabot, Trivy, or OWASP Dependency-Check).
   - The scan must analyze all third-party libraries for known CVEs and output recommended remediation steps or version bumps.
3. **AI-Powered Code Review:**
   - Utilize an AI code review integration (e.g., a compatible GitHub Action or direct API integration using available free AI tools on GitHub).
   - The AI reviewer must analyze the changed files (`git diff`), assess code quality, identify potential bugs/vulnerabilities, and post automated review comments with actionable suggestions for code improvement.

## 3. Release Automation & Artifact Preparation
When a release process is initiated, a dedicated CD pipeline (`.github/workflows/release.yml`) must be triggered to handle the build and versioning process.
- **Artifact Preparation:** The script must compile and build the production-ready artifacts for the respective repository (e.g., Android APK/AAB, iOS IPA, Web static files, or Server Docker images).
- **Changelog & Versioning Script:** The AI must implement a script (triggered by the release action) that:
  1. Parses the `Changelog.md` file to extract the data under the `[Unreleased]` section.
  2. Determines the appropriate version number for the new release.
  3. Uses the extracted data to automatically populate the GitHub Release notes.
  4. Automatically updates the version numbers in project configuration files (e.g., `build.gradle.kts`, `package.json`).
  5. Updates `Changelog.md` by replacing `[Unreleased]` with the new version number/date, and prepares a fresh `[Unreleased]` header for future development.
