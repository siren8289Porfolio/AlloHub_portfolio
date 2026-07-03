#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-production}"

echo "Blue-Green rollback: switching traffic to blue"
kubectl patch service allochub-api \
  -p '{"spec":{"selector":{"app":"allochub-api","version":"blue"}}}' \
  -n "${NAMESPACE}"

curl -sf "https://api.example.com/api/health" && echo "Health check passed"

echo "Removing green deployment"
kubectl delete deployment allochub-api-green -n "${NAMESPACE}" --ignore-not-found
