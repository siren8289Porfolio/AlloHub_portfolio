#!/usr/bin/env bash
set -euo pipefail

IMAGE="${1:-allochub:latest}"
NAMESPACE="${2:-staging}"

echo "Deploying ${IMAGE} to staging (${NAMESPACE})"
kubectl apply -f deploy/kubernetes/namespace.yaml
kubectl apply -f deploy/kubernetes/staging-deployment.yaml -f deploy/kubernetes/staging-service.yaml -n "${NAMESPACE}"
kubectl set image deployment/allochub-api allochub-api="${IMAGE}" -n "${NAMESPACE}"
kubectl rollout status deployment/allochub-api -n "${NAMESPACE}" --timeout=120s

BASE_URL="${STAGING_URL:-https://staging-api.example.com}"
./deploy/scripts/smoke-test.sh "${BASE_URL}" "${ALLOC_OPERATOR_TOKEN:-operator-dev-token}"
