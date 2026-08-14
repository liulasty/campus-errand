#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

ENV=${1:-dev}
VERSION=${2:-$(bash scripts/version.sh)}

ENV_FILE="config/.env.${ENV}"
COMPOSE_FILES=(-f docker-compose.yml -f "docker-compose.${ENV}.yml")

read_env() {
  grep -E "^[[:space:]]*${1}=" "$2" 2>/dev/null | tail -1 | sed -E 's/^[^=]*=//' | tr -d '\r'
}
APP_NAME=$(read_env APP_NAME "$ENV_FILE"); APP_NAME=${APP_NAME:-campus_entrustment}
REGISTRY=$(read_env REGISTRY "$ENV_FILE")

export APP_NAME
export IMAGE_TAG=${VERSION}

echo "🚀 Deploying ${APP_NAME}-backend:${VERSION} [${ENV}]..."

if [ -n "${REGISTRY}" ]; then
  docker compose "${COMPOSE_FILES[@]}" --env-file "${ENV_FILE}" pull
else
  echo "ℹ️  REGISTRY 为空，跳过 pull，使用本地镜像。"
fi

docker compose "${COMPOSE_FILES[@]}" --env-file "${ENV_FILE}" up -d --remove-orphans

echo "✅ Done. Services:"
docker compose "${COMPOSE_FILES[@]}" ps
