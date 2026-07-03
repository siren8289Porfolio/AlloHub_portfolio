-- 운영 필수 seed (전체 초기화 없이 실행 가능)
-- 사용: docker compose exec -T db psql -U $DB_USER -d $DB_NAME < back/db/seed/ops.sql
-- 또는: ./back/db/scripts/seed-ops.sh

-- 운영 필수 데이터는 현재 없음 (스키마만으로 기동).
-- 토큰/권한은 환경 변수 ALLOC_OPERATOR_TOKEN / ALLOC_ADMIN_TOKEN 으로 관리.
SELECT 1;
