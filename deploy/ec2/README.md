# AlloHub EC2 배포 (MVP)

Pivot과 동일한 순서: **DB → Back → Front → Nginx**, 그다음 GitHub Actions.

## 1. 배포 단위

```txt
front
back
db
nginx
docker-compose.yml
github actions
.env
```

## 2. EC2 폴더

```bash
cd /home/ubuntu
git clone https://github.com/siren8289Porfolio/AlloHub_portfolio.git allohub
cd allohub
```

## 3. `.env` 먼저

```bash
cp .env.example .env
nano .env
```

필수:

```env
DB_NAME=allohub
DB_USER=allohub
DB_PASSWORD=강한비밀번호
SPRING_PROFILES_ACTIVE=prod
ALLOC_OPERATOR_TOKEN=...
ALLOC_ADMIN_TOKEN=...
```

> GitHub Actions 실패의 대부분이 `.env` 누락입니다.

## 4. 구조

```txt
allohub/
  docker-compose.yml
  .env
  nginx/default.conf
  back/
  front/
  back/db/migration/   # Flyway 복사본 (문서/수동)
  back/db/seed/        # ops / demo seed
```

Flyway 실제 경로: `back/src/main/resources/db/migration` (기동 시 자동 적용)

## 5~8. 수동 기동 순서

```bash
# 5. DB
docker compose up -d db
docker compose logs -f db

# 6. Back (Flyway V1 적용)
docker compose up -d back
docker compose logs -f back

# 7. Front
docker compose up -d front
docker compose logs -f front

# 8. Nginx
docker compose up -d nginx
docker compose logs -f nginx
```

한 번에:

```bash
docker compose up -d --build
```

## 9. 헬스체크

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/health
curl http://localhost/allohub/
curl http://localhost/allohub/api/health
docker compose ps
```

브라우저: `http://<EC2_PUBLIC_IP>/allohub/`

## 10. Seed (선택)

전체 초기화 없이 데모 데이터만:

```bash
chmod +x back/db/scripts/*.sh
./back/db/scripts/seed-ops.sh
./back/db/scripts/seed-demo.sh
```

## 11. GitHub Actions Secrets

Repo → Settings → Secrets and variables → Actions:

| Secret | 값 |
|--------|-----|
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | pem **전체** 내용 |

`main` push 시:

```bash
cd /home/ubuntu/allohub
git reset --hard origin/main
docker compose up -d --build
docker image prune -f
```

## 보안 그룹

| 포트 | 용도 |
|------|------|
| 22 | SSH |
| 80 | Nginx (UI + API) |
| 8080 | Back 직접 헬스 (선택) |

## 롤백

```bash
cd /home/ubuntu/allohub
git log --oneline -5
git reset --hard <이전-커밋>
docker compose up -d --build
```
