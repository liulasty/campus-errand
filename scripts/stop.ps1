param([string]$Env = "dev")
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot\..

$composeArgs = @("-f", "docker-compose.yml", "-f", "docker-compose.$Env.yml", "--env-file", "config\.env.$Env")

Write-Host "🛑 Stopping campus_entrustment [$Env]..." -ForegroundColor Cyan
& docker compose @composeArgs down
if ($LASTEXITCODE -ne 0) { throw "docker compose down failed" }
Write-Host "✅ Stopped." -ForegroundColor Green
