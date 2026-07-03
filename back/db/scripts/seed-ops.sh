#!/usr/bin/env bash
# 운영 필수 seed (전체 초기화 없음)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"

DB_USER="${DB_USER:-allohub}"
DB_NAME="${DB_NAME:-allohub}"

echo "==> Seed ops → ${DB_NAME}"
docker compose exec -T db psql -U "${DB_USER}" -d "${DB_NAME}" < back/db/seed/ops.sql
echo "==> Done"
