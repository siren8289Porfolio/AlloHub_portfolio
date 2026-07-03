#!/usr/bin/env bash
# Operation Manual 13.2 — DB 백업 및 복구
set -euo pipefail

DB_INSTANCE="${DB_INSTANCE:-allochub-db}"
SNAPSHOT_ID="${1:-}"

echo "==> Runbook 13: Backup & Restore"
echo ""

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI required for RDS operations"
  exit 1
fi

echo "--- 최신 스냅샷 목록"
aws rds describe-db-snapshots \
  --db-instance-identifier "${DB_INSTANCE}" \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
  --output table

if [ -n "${SNAPSHOT_ID}" ]; then
  RESTORE_ID="${DB_INSTANCE}-restore-$(date +%Y%m%d)"
  echo ""
  echo "--- 스냅샷 복구: ${SNAPSHOT_ID} → ${RESTORE_ID}"
  aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier "${RESTORE_ID}" \
    --db-snapshot-identifier "${SNAPSHOT_ID}"

  echo ""
  echo "--- 복구 후 Connection String 변경"
  echo "kubectl set env deployment/allochub-api-green \\"
  echo "  DATABASE_URL=postgresql://admin:PASSWORD@${RESTORE_ID}:5432/allochub \\"
  echo "  -n production"
else
  echo ""
  echo "Usage: $0 <snapshot-id>"
  echo "Example: $0 allochub-db-snapshot-2026-07-02"
fi
