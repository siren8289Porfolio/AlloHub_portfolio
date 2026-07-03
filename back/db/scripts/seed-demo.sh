#!/usr/bin/env bash
# 데모 데이터만 재삽입 (전체 DB 초기화 없음, 멱등)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"

DB_USER="${DB_USER:-allohub}"
DB_NAME="${DB_NAME:-allohub}"

echo "==> Seed demo → ${DB_NAME}"
docker compose exec -T db psql -U "${DB_USER}" -d "${DB_NAME}" < back/db/seed/demo.sql
echo "==> Done"
