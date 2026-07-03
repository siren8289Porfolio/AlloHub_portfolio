#!/usr/bin/env bash
# Operation Manual 10.3 — 배포 실패 시 조치
set -euo pipefail

NAMESPACE="${1:-production}"

echo "==> Runbook 10.3: 배포 실패"
echo ""

echo "--- [진단 1] GitHub Actions — 최근 워크플로 확인"
echo "https://github.com/<org>/AlloHub_Portfolio/actions"

echo ""
echo "--- [진단 2] 현재 배포 상태"
kubectl get deployment -n "${NAMESPACE}" -l app=allochub-api 2>/dev/null || echo "kubectl unavailable"
kubectl rollout status deployment/allochub-api-green -n "${NAMESPACE}" 2>/dev/null || true

echo ""
echo "--- [대응 1] GitHub Actions 수동 취소 (진행 중인 실패 배포)"

echo ""
echo "--- [대응 2] 실행 중 버전 유지 — 롤백하지 않음 (이미 서비스 중인 경우)"

echo ""
echo "--- [대응 3] 원인별 조치"
echo "  - 테스트 실패 → 코드 수정 후 PR"
echo "  - 빌드 실패 → npm ci && npm run build 로컬 재현"
echo "  - 배포 실패 → Secrets/환경변수 확인 (deploy/kubernetes/secrets.example.yaml)"

echo ""
echo "--- [대응 4] 수정 후 재배포"
echo "git push origin main  # CI/CD 자동 트리거"
echo "./deploy/scripts/deploy-staging.sh ghcr.io/<org>/allohub:<sha>"
