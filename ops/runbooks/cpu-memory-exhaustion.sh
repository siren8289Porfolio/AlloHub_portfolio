#!/usr/bin/env bash
# Operation Manual 10.4 — CPU/메모리 고갈 시 조치
set -euo pipefail

NAMESPACE="${1:-production}"
DEPLOYMENT="${2:-allochub-api-green}"

echo "==> Runbook 10.4: CPU/메모리 고갈"
echo ""

echo "--- [진단] Pod 리소스 사용량"
kubectl top pods -n "${NAMESPACE}" -l app=allochub-api 2>/dev/null || echo "metrics-server required"

echo ""
echo "--- [진단] HPA 상태"
kubectl get hpa -n "${NAMESPACE}" 2>/dev/null || echo "HPA not configured — see deploy/kubernetes/hpa.yaml"

echo ""
echo "--- [대응 1] Pod 재시작"
echo "kubectl rollout restart deployment/${DEPLOYMENT} -n ${NAMESPACE}"

echo ""
echo "--- [대응 2] 레플리카 증가"
echo "kubectl scale deployment/${DEPLOYMENT} --replicas=5 -n ${NAMESPACE}"

echo ""
echo "--- [대응 3] 느린 쿼리 (PostgreSQL)"
echo "SELECT pid, query, state, query_start FROM pg_stat_activity WHERE state != 'idle';"
echo "-- 필요 시: SELECT pg_terminate_backend(pid);"

echo ""
echo "--- [대응 4] Docker Dev 환경"
echo "docker stats --no-stream"
echo "docker compose -f docker-compose.dev.yml restart api"
