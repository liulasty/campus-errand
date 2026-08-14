#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Image name mirrors docker-compose interpolation: ${REGISTRY}/${APP_NAME}-backend:${TAG}
read_env() {
  grep -E "^[[:space:]]*${1}=" "$2" 2>/dev/null | tail -1 | sed -E 's/^[^=]*=//' | tr -d '\r'
}
ENV_FILE="config/.env"
APP_NAME=$(read_env APP_NAME "$ENV_FILE"); APP_NAME=${APP_NAME:-campus_entrustment}
REGISTRY=$(read_env REGISTRY "$ENV_FILE")

TAG=${1:-$(bash scripts/version.sh)}
IMAGE="${REGISTRY:+$REGISTRY/}${APP_NAME}-backend:${TAG}"

echo "🚀 Building ${IMAGE}..."
docker build -t "${IMAGE}" -f docker/backend/Dockerfile .
echo "✅ Build complete: ${IMAGE}"
