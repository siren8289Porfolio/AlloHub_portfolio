#!/usr/bin/env bash
# Operation Manual 8.2 — 주간 점검 (목표 30분)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATE="$(date '+%Y-%m-%d')"

echo "=========================================="
echo " AllocHub Weekly Check — ${DATE}"
echo "=========================================="

echo ""
echo "[1/6] 일일 점검 실행"
"${SCRIPT_DIR}/daily-check.sh" || true

echo ""
echo "[2/6] 의존성 취약점 (npm audit)"
npm audit --audit-level=high || echo "    ⚠️  audit findings — review required"

echo ""
echo "[3/6] 테스트 스위트"
npm test

echo ""
echo "[4/6] RDS 백업 확인 (AWS CLI 필요)"
if command -v aws >/dev/null 2>&1; then
  aws rds describe-db-snapshots \
    --db-instance-identifier allochub-db \
    --query 'DBSnapshots[0].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
    --output table 2>/dev/null || echo "    ⚠️  RDS snapshot check skipped (no AWS access)"
else
  echo "    ⚠️  aws CLI not installed — manual RDS backup check required"
fi

echo ""
echo "[5/6] 디스크 사용량"
df -h . | tail -1

echo ""
echo "[6/6] 수동 확인 항목"
echo "    [ ] 지난주 Incident 리뷰"
echo "    [ ] Slow Query 확인 (RDS Performance Insights)"
echo "    [ ] 권한 계정 리뷰 (IAM / API 토큰)"

echo ""
echo " Weekly check script complete — complete manual items above"
