param([string]$Tag = "")
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot\..

function Get-EnvVar([string]$key, [string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return "" }
    $line = Select-String -Path $path -Pattern "^[ \t]*$key=" -ErrorAction SilentlyContinue | Select-Object -Last 1
    if ($null -eq $line) { return "" }
    return ($line.Line -split "=", 2)[1].TrimEnd("`r")
}

# Image name mirrors docker-compose interpolation: ${REGISTRY}/${APP_NAME}-backend:${TAG}
$appName = Get-EnvVar "APP_NAME" "config\.env"; if (-not $appName) { $appName = "campus_entrustment" }
$registry = Get-EnvVar "REGISTRY" "config\.env"

if (-not $Tag) {
    $base = & git describe --tags --abbrev=0 2>$null
    $short = & git rev-parse --short HEAD 2>$null
    $Tag = if ($base -and $short) { "$base-$short" } else { "latest" }
}

$image = "{0}{1}-backend:{2}" -f $(if ($registry) { "$registry/" } else { "" }), $appName, $Tag
Write-Host "🚀 Building $image..." -ForegroundColor Cyan
docker build -t $image -f docker/backend/Dockerfile .
if ($LASTEXITCODE -ne 0) { throw "docker build failed" }
Write-Host "✅ Build complete: $image" -ForegroundColor Green
