#!/usr/bin/env bash
# AllocHub Smoke Test (Deployment Plan 6.1, 7.3)
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
TOKEN="${2:-operator-dev-token}"

echo "==> Smoke Test: ${BASE_URL}"

# 1. Health Check
echo "--- Health Check"
curl -sf "${BASE_URL}/api/health" | grep -q '"status":"UP"'
echo "OK"

# 2. Auth required
echo "--- Unauthorized check (TC-010)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/investors")
[[ "$STATUS" == "401" ]] || { echo "Expected 401, got ${STATUS}"; exit 1; }
echo "OK"

# 3. Investor registration
echo "--- Investor registration"
curl -sf -X POST "${BASE_URL}/api/investors" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke A","investmentAmount":100000,"allocationRatio":20}' \
  | grep -q '"success":true'
echo "OK"

# 4. Investor list
echo "--- Investor list"
curl -sf "${BASE_URL}/api/investors" \
  -H "Authorization: Bearer ${TOKEN}" \
  | grep -q '"investors"'
echo "OK"

# 5. Reconciliation
echo "--- Reconciliation"
curl -sf "${BASE_URL}/api/reconciliation" \
  -H "Authorization: Bearer ${TOKEN}" \
  | grep -q '"isValid"'
echo "OK"

echo "==> All smoke tests passed"
