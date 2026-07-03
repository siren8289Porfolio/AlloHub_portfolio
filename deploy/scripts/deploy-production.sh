#!/usr/bin/env bash
set -euo pipefail

IMAGE="${1:-allochub:v1.0.0}"
NAMESPACE="${2:-production}"

echo "Blue-Green deploy: ${IMAGE} to ${NAMESPACE}"

kubectl apply -f deploy/kubernetes/namespace.yaml
kubectl apply -f deploy/kubernetes/production-deployment.yaml -n "${NAMESPACE}"
# 또는 Green만: kubectl apply -f deploy/kubernetes/production-deployment-green.yaml -n "${NAMESPACE}"

kubectl set image deployment/allochub-api-green allochub-api="${IMAGE}" -n "${NAMESPACE}"
kubectl rollout status deployment/allochub-api-green -n "${NAMESPACE}" --timeout=180s

echo "Green pods health check"
kubectl get pods -l version=green -n "${NAMESPACE}"

BASE_URL="${PRODUCTION_URL:-https://api.example.com}"
curl -sf "${BASE_URL}/api/health" | grep -q '"status":"UP"'

echo "Switching traffic to green"
kubectl patch service allochub-api \
  -p '{"spec":{"selector":{"app":"allochub-api","version":"green"}}}' \
  -n "${NAMESPACE}"

echo "Production deployment complete. Monitor for 10 minutes before removing blue."
