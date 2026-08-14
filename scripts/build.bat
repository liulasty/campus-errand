@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\.."

set "TAG=%~1"
if "!TAG!"=="" (
  set "BASE="
  set "SHORT="
  for /f %%i in ('git describe --tags --abbrev^=0 2^>nul') do set "BASE=%%i"
  for /f %%i in ('git rev-parse --short HEAD 2^>nul') do set "SHORT=%%i"
  if defined BASE if defined SHORT set "TAG=!BASE!-!SHORT!"
)
if "!TAG!"=="" set "TAG=latest"

set "APP_NAME=campus_entrustment"
set "REGISTRY="
if exist "config\.env" (
  for /f "usebackq tokens=1,* delims==" %%a in (`findstr /b "APP_NAME=" "config\.env"`) do set "APP_NAME=%%b"
  for /f "usebackq tokens=1,* delims==" %%a in (`findstr /b "REGISTRY=" "config\.env"`) do set "REGISTRY=%%b"
)

if "!REGISTRY!"=="" (set "IMG=!APP_NAME!-backend:!TAG!") else (set "IMG=!REGISTRY!/!APP_NAME!-backend:!TAG!")

echo 🚀 Building !IMG!...
docker build -t "!IMG!" -f docker/backend/Dockerfile .
if errorlevel 1 (
  echo ❌ Build failed.
  exit /b 1
)
echo ✅ Build complete: !IMG!
