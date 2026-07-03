# AllocHub 운영 가이드 (Operation Manual v1.0)

Next.js + Prisma 스택에 맞게 조정된 운영 문서입니다.

## Spring Boot → AllocHub 매핑

| Operation Manual | AllocHub |
|------------------|----------|
| Spring Boot API | **`back/`** — `src/app/api/*` |
| React Frontend | **`front/`** — `src/app/*` |
| `/actuator/health` | **`GET /api/health`** (back:8080) |
| Actuator metrics | **`GET /api/metrics`** (back) |
| Database | **`back/dev.db`** / PostgreSQL |

## 운영 목표 (2장)

| 목표 | 기준 |
|------|------|
| 가용성 | 99% 이상 (월 7시간 이하 다운타임) |
| 정합성 | 오류 0건 (`GET /api/reconciliation`) |
| 복구 | MTTR 30분 이내 |
| 성능 | p95 < 1초 |

## 서비스 상태 (5장)

| 상태 | 기준 | 대응 |
|------|------|------|
| 🟢 Normal | API 200/201, 에러율 < 1% | 일일 점검 |
| 🟡 Warning | p95 > 1초, 에러율 1–5% | 원인 확인 |
| 🔴 Incident | API 5xx, 에러율 > 5% | Runbook 즉시 실행 |
| 🔵 Maintenance | 배포/점검 | 사전 공지 |

## 모니터링 (6장)

### Application

```bash
curl http://localhost:8080/api/health    # DB 포함 헬스
curl http://localhost:8080/api/metrics     # Prometheus 메트릭
```

| 메트릭 | 정상 | 경고 |
|--------|------|------|
| p95 응답 | < 1000ms | > 1500ms |
| 5xx 에러율 | < 1% | > 5% |
| 401 비율 | < 2% | > 10% |

### 스택 실행

```bash
docker compose -f docker-compose.dev.yml -f docker-compose.monitoring.yml up -d
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3001
```

Alert 규칙: `ops/alerts/prometheus-alerts.yml` (ALT-001 ~ ALT-008)

## 알림 (7장)

| Alert ID | 조건 | 심각도 | Runbook |
|----------|------|--------|---------|
| ALT-001 | 5xx > 10% | Critical | `ops/runbooks/api-5xx.sh` |
| ALT-002 | 에러율 > 5% | Major | `ops/runbooks/api-5xx.sh` |
| ALT-004 | Health DOWN | Critical | `ops/runbooks/db-connection-failure.sh` |
| ALT-007 | 배포 실패 | Major | `ops/runbooks/deploy-failure.sh` |
| ALT-005/006 | CPU/디스크 | Warning | `ops/runbooks/cpu-memory-exhaustion.sh` |
| ALT-008 | 인증 실패 > 20% | Warning | Security 팀 |

Alertmanager 예시: `ops/alerts/alertmanager.example.yml`

## 운영 점검 (8장)

### 일일 (9:00, 15분)

```bash
npm run ops:daily
# 또는
./ops/scripts/daily-check.sh
```

- Health Check (`/api/health`)
- 주요 API (investors, investments, reconciliation)
- 에러 로그 / 메트릭 확인

### 주간 (월요일 10:00, 30분)

```bash
npm run ops:weekly
```

### 월간 (매월 1일, 1시간)

```bash
npm run ops:monthly
```

리포트 템플릿: `ops/reports/monthly-template.md`

## Incident 관리 (9장)

### 심각도

| 등급 | 기준 | 대응 |
|------|------|------|
| SEV-1 | 전체 장애 | 즉시 (5분) |
| SEV-2 | 핵심 기능 불가 | 30분 |
| SEV-3 | 일부 기능 | 1시간 |
| SEV-4 | 경미 | 업무 시간 내 |

### 대응 흐름

```
탐지 → 등급 판단 → 온콜 통보 → 원인 조사 → 롤백/핫픽스
→ 정상화 확인 → 공지 → Postmortem (2시간 내)
```

템플릿: `ops/templates/incident-report.md`, `ops/templates/postmortem.md`

## Runbook (10장)

| 시나리오 | 스크립트 |
|----------|----------|
| API 5xx 증가 | `./ops/runbooks/api-5xx.sh production` |
| DB 연결 실패 | `./ops/runbooks/db-connection-failure.sh production` |
| 배포 실패 | `./ops/runbooks/deploy-failure.sh` |
| CPU/메모리 고갈 | `./ops/runbooks/cpu-memory-exhaustion.sh production` |
| DB 백업/복구 | `./ops/runbooks/backup-restore.sh <snapshot-id>` |

Production 롤백: `./deploy/scripts/rollback-production.sh`

## Change 관리 (12장)

변경 이력: `ops/templates/change-log.md`

```
계획 → 승인 → 공지 → Dev→Staging→Prod → 검증 → 완료
```

## 백업 (13장)

| 대상 | 주기 | 담당 |
|------|------|------|
| RDS | 일 1회 02:00 | DBA |
| K8s Secrets | 변경 시 | DevOps |
| 로그 | 90일 보관 | DevOps |

## 보안 운영 (14장)

| 항목 | 주기 | 명령 |
|------|------|------|
| 의존성 취약점 | 주 1회 | `npm audit` |
| 코드 스캔 | 배포 시 | GitHub Security Tab |
| 토큰 순환 | 분기 1회 | `ALLOC_OPERATOR_TOKEN`, `ALLOC_ADMIN_TOKEN` |

## 온콜 (17장)

```
7/1–7:   DevOps Lead
7/8–14:  Backend Engineer
7/15–21: DBA
7/22–30: QA Lead
```

에스컬레이션: On-Call (15분) → Team Lead (30분) → CTO (1시간) → CEO

## 디렉터리 구조

```
ops/
├── README.md                 # 이 문서
├── scripts/
│   ├── daily-check.sh        # 8.1 일일 점검
│   ├── daily-api-test.sh     # 8.1 API 테스트
│   ├── weekly-check.sh       # 8.2 주간 점검
│   └── monthly-check.sh      # 8.3 월간 점검
├── runbooks/                 # 10장 응급 대응
├── alerts/                   # 7장 Prometheus/Alertmanager
├── templates/                # Incident, Postmortem, Change
└── reports/                  # 월간 리포트
```

배포 관련: [deploy/README.md](../deploy/README.md)
