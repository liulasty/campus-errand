@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0\.."

set "ENV=%~1"
if "!ENV!"=="" set "ENV=dev"

set "VERSION=%~2"
if "!VERSION!"=="" (
  set "BASE="
  set "SHORT="
  for /f %%i in ('git describe --tags --abbrev^=0 2^>nul') do set "BASE=%%i"
  for /f %%i in ('git rev-parse --short HEAD 2^>nul') do set "SHORT=%%i"
  if defined BASE if defined SHORT set "VERSION=!BASE!-!SHORT!"
)
if "!VERSION!"=="" set "VERSION=latest"

set "ENV_FILE=config\.env.!ENV!"

set "APP_NAME=campus_entrustment"
set "REGISTRY="
if exist "!ENV_FILE!" (
  for /f "usebackq tokens=1,* delims==" %%a in (`findstr /b "APP_NAME=" "!ENV_FILE!"`) do set "APP_NAME=%%b"
  for /f "usebackq tokens=1,* delims==" %%a in (`findstr /b "REGISTRY=" "!ENV_FILE!"`) do set "REGISTRY=%%b"
)
set "IMAGE_TAG=!VERSION!"

echo 🚀 Deploying !APP_NAME!-backend:!VERSION! [!ENV!]...

if "!REGISTRY!"=="" (
  echo ℹ️ REGISTRY 为空，跳过 pull，使用本地镜像。
) else (
  docker compose -f docker-compose.yml -f docker-compose.!ENV!.yml --env-file !ENV_FILE! pull
  if errorlevel 1 exit /b 1
)

docker compose -f docker-compose.yml -f docker-compose.!ENV!.yml --env-file !ENV_FILE! up -d --remove-orphans
if errorlevel 1 exit /b 1

echo ✅ Done. Services:
docker compose -f docker-compose.yml -f docker-compose.!ENV!.yml --env-file !ENV_FILE! ps
