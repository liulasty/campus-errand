@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\.."

set "ENV=%~1"
if "!ENV!"=="" set "ENV=dev"

echo 🛑 Stopping campus_entrustment [!ENV!]...
docker compose -f docker-compose.yml -f docker-compose.!ENV!.yml --env-file config\.env.!ENV! down
if errorlevel 1 exit /b 1
echo ✅ Stopped.
