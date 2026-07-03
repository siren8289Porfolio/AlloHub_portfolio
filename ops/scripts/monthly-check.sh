#!/usr/bin/env bash
# Operation Manual 8.3 — 월간 점검 (목표 1시간)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONTH="$(date '+%Y-%m')"

echo "=========================================="
echo " AllocHub Monthly Check — ${MONTH}"
echo "=========================================="

"${SCRIPT_DIR}/weekly-check.sh"

echo ""
echo "[월간 추가 항목]"
echo "    [ ] Availability 달성률 (목표 99%)"
echo "    [ ] 에러율 / 응답 시간 추이 (Grafana/Datadog)"
echo "    [ ] AWS 비용 검토"
echo "    [ ] DB 인덱스 성능 점검"
echo "    [ ] 미해결 Bug/Incident 상태"
echo "    [ ] 운영 문서 최신화 (ops/README.md)"
echo "    [ ] 다음 달 배포 계획"

if [ -f "ops/reports/monthly-template.md" ]; then
  REPORT="ops/reports/monthly-${MONTH}.md"
  if [ ! -f "${REPORT}" ]; then
    cp ops/reports/monthly-template.md "${REPORT}"
    echo ""
    echo " Created report template: ${REPORT}"
  fi
fi

echo ""
echo " Monthly check script complete"
