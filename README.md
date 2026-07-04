# AllocHub

출자자 → 운용사 → 기업까지 자금 흐름의 **정합성을 유지**하며 각 단계를 자동 계산하는 MVP입니다.

## 구조

```txt
allohub/
  front/          # Next.js UI (:3000, EC2: /allohub/)
  back/           # Spring Boot API (:8080)
  nginx/          # 리버스 프록시 (:80)
  docker-compose.yml
  .env.example
  back/db/        # migration 복사본 + seed
```

## 로컬 개발

```bash
npm install
cd back && ./gradlew bootRun   # :8080
npm run dev:front              # :3000, /api → back rewrite
```

토큰: `operator-dev-token` / `admin-dev-token`

## EC2 배포 (MVP)

순서: **`.env` → DB → Back → Front → Nginx → Actions**

```bash
# EC2 — 경로: /home/ubuntu/my-portfolio
cd /home/ubuntu/my-portfolio
cp .env.example .env && nano .env

docker compose config --services   # allohub-app, allohub-web 확인
docker compose up -d --build

curl http://localhost:8080/actuator/health
curl http://localhost/allohub/
```

데모 데이터 (전체 초기화 없음):

```bash
./back/db/scripts/seed-demo.sh
```

상세: [deploy/ec2/README.md](deploy/ec2/README.md)

## GitHub Actions Secrets

| Secret | 설명 |
|--------|------|
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | pem 전체 |

`main` push → SSH → `docker compose up -d --build`

## 테스트 (PostgreSQL)

운영과 동일하게 **PostgreSQL**로 통합 테스트합니다. (SQLite 테스트 제거)

```bash
# 로컬 (호스트 5432가 점유된 경우 5433)
npm run test:pg

# CI: GitHub Actions services.postgres (localhost:5432)
cd back && ./gradlew test
```

