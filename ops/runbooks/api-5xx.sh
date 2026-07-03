#!/usr/bin/env bash
# Operation Manual 10.1 — API 5xx 증가 시 조치
set -euo pipefail

NAMESPACE="${1:-production}"
BASE_URL="${BASE_URL:-https://api.example.com}"

echo "==> Runbook 10.1: API 5xx 증가"
echo ""

echo "--- [진단] 메트릭 확인"
curl -sf "${BASE_URL}/api/metrics" 2>/dev/null | grep -E "error_rate|server_errors" || echo "Metrics unavailable"

echo ""
echo "--- [진단] 최근 배포 이력"
kubectl rollout history deployment/allochub-api-green -n "${NAMESPACE}" 2>/dev/null || \
kubectl rollout history deployment/allochub-api -n staging 2>/dev/null || \
echo "kubectl not available — check GitHub Actions"

echo ""
echo "--- [대응 1] Blue-Green 롤백 (Production)"
echo "Run: ./deploy/scripts/rollback-production.sh ${NAMESPACE}"

echo ""
echo "--- [대응 2] Rolling 롤백 (Staging)"
echo "Run: ./deploy/scripts/rollback-staging.sh staging"

echo ""
echo "--- [대응 3] Health Check"
curl -sf "${BASE_URL}/api/health" && echo "Health OK" || echo "Health FAILED"

echo ""
echo "--- [대응 4] 에러 로그 수집"
kubectl logs -l app=allochub-api,version=blue -n "${NAMESPACE}" --tail=200 > /tmp/allochub-error-$(date +%Y%m%d).log 2>/dev/null || \
docker compose -f docker-compose.dev.yml logs --tail=200 api > /tmp/allochub-error-$(date +%Y%m%d).log 2>/dev/null || \
echo "Log collection skipped"

echo "==> Runbook steps listed — execute rollback if error rate > 5%"
