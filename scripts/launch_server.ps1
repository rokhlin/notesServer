<#
.SYNOPSIS
    Automated launcher for NotesAlltogether Ktor Backend Server.
.DESCRIPTION
    Ensures local storage directory exists and launches Ktor Netty microservice on port 8080.
#>

[CmdletBinding()]
param(
    [int]$Port = 8080,
    [string]$StorageDir = "data/vault"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   NotesAlltogether - Backend Server Launch Script        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $ProjectRoot

try {
    # 1. Ensure local storage vault directory exists
    $VaultPath = Join-Path $ProjectRoot $StorageDir
    if (-not (Test-Path $VaultPath)) {
        New-Item -ItemType Directory -Path $VaultPath -Force | Out-Null
        Write-Host "[1/2] Initialized local server vault directory at: $VaultPath" -ForegroundColor Green
    } else {
        Write-Host "[1/2] Local server vault directory verified at: $VaultPath" -ForegroundColor Green
    }

    # 2. Launch Ktor Server
    Write-Host "[2/2] Launching Ktor Server on http://0.0.0.0:$Port ..." -ForegroundColor Yellow
    Write-Host "Endpoints:" -ForegroundColor Gray
    Write-Host "  - Notes API:      http://localhost:$Port/api/v1/notes" -ForegroundColor Gray
    Write-Host "  - Sync Engine:    http://localhost:$Port/api/v1/sync" -ForegroundColor Gray
    Write-Host "  - WebSockets:     ws://localhost:$Port/ws/collab/{noteId}" -ForegroundColor Gray
    Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Gray

    $gradlew = Join-Path $ProjectRoot "gradlew.bat"
    & $gradlew run
} finally {
    Pop-Location
}
