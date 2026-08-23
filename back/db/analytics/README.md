# AlloHub Data Engineering SQL Baseline

이 디렉터리는 운영 OLTP 원장을 대체하지 않는 분석/QA용 SQL 산출물이다.

## Files

- `01_quality_checks.sql`: 운영 원장 source에 대한 DQ/business invariant check query
- `02_analytics_marts.sql`: `analytics` schema의 read-only mart view 정의

## Run Order

```bash
psql "$DATABASE_URL" -f back/db/analytics/01_quality_checks.sql
psql "$DATABASE_URL" -f back/db/analytics/02_analytics_marts.sql
```

`01_quality_checks.sql`은 결과 행의 `status`가 모두 `PASS`일 때만 mart publish를 진행한다. `02_analytics_marts.sql`은 운영 table을 변경하지 않고 `analytics` schema에 view만 생성한다.

## Boundary

- Write authority는 PostgreSQL OLTP 원장 table에 있다.
- Analytics mart는 DA/QA/reporting 소비용이다.
- DQ fail 상태에서는 mart를 신뢰 가능한 publish 결과로 취급하지 않는다.
