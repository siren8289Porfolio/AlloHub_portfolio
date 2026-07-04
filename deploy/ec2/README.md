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

서버 경로: **`/home/ubuntu/my-portfolio`** (다른 프로젝트와 동일)

```bash
cd /home/ubuntu/my-portfolio
# AlloHub 코드가 이 디렉터리(또는 하위)에 있어야 함
ls -al
docker compose config --services
# 기대: db, allohub-app, allohub-web, nginx
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
docker compose up -d allohub-app
docker compose logs -f allohub-app

# 7. Front
docker compose up -d allohub-web
docker compose logs -f allohub-web

# 8. Nginx
docker compose up -d nginx
docker compose logs -f nginx
```

한 번에:

```bash
docker compose up -d --build
```

Actions 배포(앱만 갱신):

```bash
docker compose pull allohub-app allohub-web
docker compose up -d allohub-app allohub-web
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

`main` push 시 (debug 로그 포함):

```bash
cd /home/ubuntu/my-portfolio
docker compose config --services
docker compose pull allohub-app allohub-web
docker compose up -d allohub-app allohub-web
```

서비스명이 다르면 EC2에서 `docker compose config --services` 결과를 workflow에 그대로 반영.

## 보안 그룹

| 포트 | 용도 |
|------|------|
| 22 | SSH |
| 80 | Nginx (UI + API) |
| 8080 | Back 직접 헬스 (선택) |

## 롤백

```bash
cd /home/ubuntu/my-portfolio
git log --oneline -5
git reset --hard <이전-커밋>
docker compose up -d allohub-app allohub-web
```
