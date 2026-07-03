#!/usr/bin/env bash
# Operation Manual 10.2 — DB 연결 실패 시 조치
set -euo pipefail

NAMESPACE="${1:-production}"
DB_INSTANCE="${DB_INSTANCE:-allochub-db}"

echo "==> Runbook 10.2: DB 연결 실패"
echo ""

echo "--- [진단] Health Check (DB 포함)"
curl -sf "${BASE_URL:-https://api.example.com}/api/health" || echo "API DOWN"

echo ""
echo "--- [진단] RDS 인스턴스 상태"
if command -v aws >/dev/null 2>&1; then
  aws rds describe-db-instances \
    --db-instance-identifier "${DB_INSTANCE}" \
    --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address]' \
    --output table 2>/dev/null || echo "AWS RDS check failed"
else
  echo "aws CLI required — check RDS Console manually"
fi

echo ""
echo "--- [대응 1] API Pod 재시작"
echo "kubectl rollout restart deployment/allochub-api-green -n ${NAMESPACE}"
echo "kubectl rollout restart deployment/allochub-api -n staging"

echo ""
echo "--- [대응 2] DB Available 대기 후 헬스 확인"
echo "kubectl wait --for=condition=ready pod -l app=allochub-api -n ${NAMESPACE} --timeout=300s"

echo ""
echo "--- [대응 3] Connection String 확인"
echo "kubectl get secret allochub-db-secret -n staging -o jsonpath='{.data.url}' | base64 -d"
echo "kubectl get secret allochub-env-secret -n production -o yaml"

echo ""
echo "--- [대응 4] DB 복구 (스냅샷)"
echo "See ops/runbooks/backup-restore.sh or Operation Manual 13.2"
