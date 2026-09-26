# GitHub CI/CD & Automation Guide

This document explains the GitHub Actions workflows and configuration for `notesServer`.

## Workflows Overview

### 1. `pr-checks.yml` (Pull Request Validation & Quality)
Triggers on any Pull Request targeting `main`, `master`, or `develop`.
- **Build & Test**: Checks Gradle wrapper, compiles Kotlin JVM server sources, and runs JUnit test suites.
- **Dependency Review**: Scans dependencies for known CVEs.
- **CodeQL Analysis**: Scans Kotlin server sources for static code vulnerabilities.
- **Gemini AI Review**: Pulls the PR diff and performs automated code review using Google Gemini (`gemini-2.5-flash`), posting suggestions directly as a comment on the PR.

### 2. `release.yml` (Release Automation)
Triggers manually via GitHub Actions UI (`workflow_dispatch`).
- Parses `CHANGELOG.md` to extract unreleased changes.
- Bumps `app.version` in `gradle.properties`.
- Builds server distributions (`installDist`, `distZip`, `distTar`).
- Tags the commit, pushes version bumps to `main`, and creates a GitHub Release with assets attached.

### 3. `dependabot.yml`
- Scans Gradle dependencies and GitHub Actions workflows weekly.
- Automatically opens PRs with library updates when security fixes or updates are published.

## Required Secrets
- `GEMINI_API_KEY`: Set under GitHub repository **Settings -> Secrets and variables -> Actions** to activate AI code review.
