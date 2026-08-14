param(
    [string]$Env = "dev",
    [string]$Version = ""
)
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot\..

function Get-EnvVar([string]$key, [string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return "" }
    $line = Select-String -Path $path -Pattern "^[ \t]*$key=" -ErrorAction SilentlyContinue | Select-Object -Last 1
    if ($null -eq $line) { return "" }
    return ($line.Line -split "=", 2)[1].TrimEnd("`r")
}

$envFile = "config\.env.$Env"
$composeArgs = @("-f", "docker-compose.yml", "-f", "docker-compose.$Env.yml", "--env-file", $envFile)

$appName = Get-EnvVar "APP_NAME" $envFile; if (-not $appName) { $appName = "campus_entrustment" }
$registry = Get-EnvVar "REGISTRY" $envFile

if (-not $Version) {
    $base = & git describe --tags --abbrev=0 2>$null
    $short = & git rev-parse --short HEAD 2>$null
    $Version = if ($base -and $short) { "$base-$short" } else { "latest" }
}

$env:APP_NAME = $appName
$env:IMAGE_TAG = $Version

Write-Host "🚀 Deploying ${appName}-backend:${Version} [$Env]..." -ForegroundColor Cyan

if ($registry) {
    & docker compose @composeArgs pull
    if ($LASTEXITCODE -ne 0) { throw "docker compose pull failed" }
} else {
    Write-Host "REGISTRY empty, skip pull; using local image." -ForegroundColor Yellow
}

& docker compose @composeArgs up -d --remove-orphans
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

Write-Host "✅ Done. Services:" -ForegroundColor Green
& docker compose @composeArgs ps
