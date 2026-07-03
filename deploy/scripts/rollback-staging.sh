#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-staging}"

echo "Rolling back staging deployment in namespace: ${NAMESPACE}"
kubectl rollout undo deployment/allochub-api -n "${NAMESPACE}"
kubectl rollout status deployment/allochub-api -n "${NAMESPACE}"
curl -sf "https://staging-api.example.com/api/health" && echo "Health check passed"
