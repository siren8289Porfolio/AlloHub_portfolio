# AllocHub 배포 가이드 (Deployment Plan v1.0)

Next.js + Prisma 스택에 맞게 조정된 배포 문서입니다.

## Spring Boot → AllocHub 매핑

| Deployment Plan | AllocHub |
|-----------------|----------|
| Maven / JUnit | **`back/`** — `./gradlew test` (JUnit 5) |
| API Server | **Spring Boot** — `back/` (포트 **8080**) |
| Frontend | **`front/`** (포트 **3000**) |
| Database | **`back/prisma/`**, `back/dev.db` |

## 배포 아키텍처

```
GitHub → GitHub Actions → GHCR (Docker Registry)
  → Dev (docker-compose) / Staging (K8s) / Prod (Blue-Green)
  → PostgreSQL 14 + Prometheus/Grafana (선택)
```

## 환경별 구성

| 환경 | 파일 | DB | URL |
|------|------|-----|-----|
| Dev | `docker-compose.dev.yml` | PostgreSQL 14 | localhost:**8080** |
| Dev (SQLite) | `docker-compose.local.yml` | SQLite | localhost:3000 |
| Staging | `deploy/kubernetes/staging-deployment.yaml` | RDS | staging-api.example.com |
| Production | `deploy/kubernetes/production-deployment.yaml` | RDS Multi-AZ | api.example.com |

## Dev 배포 (6.1절 — 자동)

```bash
docker compose -f docker-compose.dev.yml pull   # CI에서 push된 이미지 사용 시
docker compose -f docker-compose.dev.yml up -d

# Health Check (Deployment Plan 6.1)
curl http://localhost:8080/api/health

# Smoke Test
./deploy/scripts/smoke-test.sh http://localhost:8080

# 로그
docker compose -f docker-compose.dev.yml logs -f api
```

## Staging 배포 (6.2절)

```bash
kubectl get pods -n staging
./deploy/scripts/deploy-staging.sh ghcr.io/<org>/allohub:<sha> staging
# 롤백
./deploy/scripts/rollback-staging.sh staging
```

## Production Blue-Green (6.3절)

```bash
./deploy/scripts/deploy-production.sh ghcr.io/<org>/allohub:v1.0.0 production

# 트래픽 전환 (스크립트 내 자동)
# 롤백
./deploy/scripts/rollback-production.sh production
```

## CI/CD 파이프라인 (5.1절)

| 단계 | 구현 |
|------|------|
| Build | `npm run build` |
| Unit/Integration Test | `npm test` |
| Security Scan | `npm audit` |
| Docker Image | GHCR push |
| Dev 배포 | `develop` push → smoke test |
| Staging | `main` → environment 승인 |
| Production | Blue-Green 수동 스크립트 |

워크플로: `.github/workflows/ci-cd.yml`

## 모니터링 (10장)

```bash
docker compose -f docker-compose.dev.yml -f docker-compose.monitoring.yml up -d
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3001 (admin/admin)
```

| 메트릭 | 정상 | 경고 |
|--------|------|------|
| CPU | < 60% | > 80% |
| 메모리 | < 70% | > 85% |
| p95 응답 | < 1000ms | > 1500ms |
| 에러율 | < 1% | > 5% |

## 배포 체크리스트 (7장)

### 배포 전
- [ ] 코드 리뷰 완료
- [ ] `npm test` 통과
- [ ] 배포 노트 작성
- [ ] 롤백 계획 확인 (`deploy/scripts/rollback-*.sh`)
- [ ] DB 마이그레이션 / `prisma migrate deploy`
- [ ] Secrets 설정 (`deploy/kubernetes/secrets.example.yaml`)

### 배포 중
- [ ] CI/CD 파이프라인 모니터링
- [ ] Health Check 통과
- [ ] Smoke Test 통과

### 배포 후
- [ ] `/api/health` → 200 OK
- [ ] 주요 API Smoke Test
- [ ] Grafana/CloudWatch 대시보드 확인
- [ ] 배포 완료 보고

## 환경 변수

| 변수 | 설명 |
|------|------|
| `DATABASE_URL` | PostgreSQL 또는 SQLite |
| `ALLOC_OPERATOR_TOKEN` | 운용사 Bearer 토큰 |
| `ALLOC_ADMIN_TOKEN` | 관리자 Bearer 토큰 |
| `PORT` | 앱 포트 (기본 3000) |

## DB 롤백 (9.3절)

```bash
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier allochub-db-rollback \
  --db-snapshot-identifier allochub-db-snapshot-YYYY-MM-DD

kubectl set env deployment/allochub-api-green \
  DATABASE_URL=<new-url> -n production
```

## 배포 일정 (11장)

| 날짜 | 단계 |
|------|------|
| 2026-07-09 | 개발 완료 |
| 2026-07-10~12 | QA 테스트 |
| 2026-07-16 | Dev 배포 |
| 2026-07-17~19 | Staging |
| 2026-07-23 | Production |
