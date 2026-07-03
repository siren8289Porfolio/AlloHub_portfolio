# DB migration & seed

| 경로 | 용도 |
|------|------|
| `src/main/resources/db/migration/` | **Flyway 실행 경로** (prod 기동 시 자동) |
| `db/migration/` | EC2/문서용 복사본 |
| `db/seed/ops.sql` | 운영 필수 seed |
| `db/seed/demo.sql` | 데모 데이터 (멱등, 재실행 가능) |
| `db/scripts/seed-*.sh` | seed 실행 스크립트 |

```bash
# 스키마: Spring Boot prod 기동 시 Flyway
# 데모만 다시 넣기 (전체 초기화 없음)
./back/db/scripts/seed-demo.sh
```
