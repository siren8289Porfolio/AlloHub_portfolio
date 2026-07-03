#!/usr/bin/env bash
# Operation Manual 8.1 — 일일 점검 (목표 15분)
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
NAMESPACE="${NAMESPACE:-production}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATE="$(date '+%Y-%m-%d %H:%M:%S')"

echo "=========================================="
echo " AllocHub Daily Check — ${DATE}"
echo "=========================================="

FAIL=0

check() {
  local name="$1"
  shift
  echo ""
  echo "[ ] ${name}"
  if "$@"; then
    echo "    ✅ PASS"
  else
    echo "    ❌ FAIL"
    FAIL=$((FAIL + 1))
  fi
}

check "Health Check — GET /api/health" \
  curl -sf "${BASE_URL}/api/health" | grep -q '"status":"UP"'

check "주요 API 테스트 (8.1)" \
  "${SCRIPT_DIR}/daily-api-test.sh" "${BASE_URL}"

if command -v kubectl >/dev/null 2>&1; then
  check "K8s Pod 상태 (production)" \
    kubectl get pods -n "${NAMESPACE}" -l app=allochub-api 2>/dev/null | grep -q Running || false
fi

if [ -f "docker-compose.dev.yml" ] && docker compose -f docker-compose.dev.yml ps api 2>/dev/null | grep -q Up; then
  check "Docker API 컨테이너 로그 — 최근 ERROR 없음" \
    bash -c '! docker compose -f docker-compose.dev.yml logs --tail=100 api 2>/dev/null | grep -qiE "ERROR|FATAL"'
fi

check "메트릭 엔드포인트" \
  curl -sf "${BASE_URL}/api/metrics" | grep -q "allochub_http_requests_total"

echo ""
echo "=========================================="
if [ "${FAIL}" -eq 0 ]; then
  echo " Daily check complete — all passed"
  exit 0
else
  echo " Daily check complete — ${FAIL} failure(s)"
  exit 1
fi
