#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

ENV=${1:-dev}
COMPOSE_FILES=(-f docker-compose.yml -f "docker-compose.${ENV}.yml")

echo "🛑 Stopping campus_entrustment [${ENV}]..."
docker compose "${COMPOSE_FILES[@]}" --env-file "config/.env.${ENV}" down
echo "✅ Stopped."
