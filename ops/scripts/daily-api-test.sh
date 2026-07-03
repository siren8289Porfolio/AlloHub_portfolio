#!/usr/bin/env bash
# Operation Manual 8.1 — 주요 API 일일 점검
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
TOKEN="${2:-operator-dev-token}"
SUFFIX="${RANDOM}"

echo "==> Daily API Test: ${BASE_URL}"

echo "--- Health Check (8.1)"
curl -sf "${BASE_URL}/api/health" | grep -q '"status":"UP"'
echo "OK"

echo "--- POST /api/investors"
curl -sf -X POST "${BASE_URL}/api/investors" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Daily-${SUFFIX}\",\"investmentAmount\":100000,\"allocationRatio\":20}" \
  | grep -q '"success":true'
echo "OK"

echo "--- GET /api/investors"
curl -sf "${BASE_URL}/api/investors" \
  -H "Authorization: Bearer ${TOKEN}" \
  | grep -q '"investors"'
echo "OK"

echo "--- POST /api/investments"
curl -sf -X POST "${BASE_URL}/api/investments" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"companyName\":\"DailyCo-${SUFFIX}\",\"investmentAmount\":50000}" \
  | grep -q '"success":true'
echo "OK"

echo "--- GET /api/reconciliation"
curl -sf "${BASE_URL}/api/reconciliation" \
  -H "Authorization: Bearer ${TOKEN}" \
  | grep -q '"isValid"'
echo "OK"

echo "==> Daily API tests passed"
