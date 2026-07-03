# AllocHub

출자자 → 운용사 → 기업까지 자금이 흐르는 과정에서 **정합성을 유지하면서 각 단계를 자동으로 계산**하는 MVP 플랫폼입니다.

## 프로젝트 구조

```
AlloHub_Portfolio/
├── back/                 # Spring Boot API (Gradle, 포트 8080)
│   ├── build.gradle.kts
│   ├── data/             # SQLite DB (dev.db, test.db)
│   └── src/main/java/com/allochub/
├── front/                # Next.js UI (포트 3000)
├── deploy/
└── ops/
```

## 기술 스택

| 영역 | 기술 |
|------|------|
| **back** | Spring Boot 3.4, Gradle, JPA, SQLite / PostgreSQL |
| **front** | Next.js 16, React 19, Tailwind CSS 4 |
| **테스트** | JUnit 5 (back), Vitest 제거 |

## 시작하기

```bash
npm install

# API 서버 (Spring Boot)
cd back && ./gradlew bootRun

# UI (별도 터미널)
npm run dev:front

# 또는 동시 실행
npm run dev
```

| 서비스 | URL |
|--------|-----|
| UI | http://localhost:3000 |
| API | http://localhost:8080/api/health |

로그인 토큰: `operator-dev-token` / `admin-dev-token`

## 테스트

```bash
cd back && ./gradlew test
# 또는
npm test
```

## DB 위치

SQLite 개발 DB: `back/data/dev.db`  
테스트 DB: `back/data/test.db`

PostgreSQL (Docker):

```bash
npm run docker:dev
```

## API 응답 형식

```json
{ "success": true, "data": {...}, "message": "요청이 성공했습니다." }
{ "success": false, "errorCode": "INVALID_INPUT", "message": "..." }
```

배포: [deploy/README.md](deploy/README.md)  
운영: [ops/README.md](ops/README.md)
