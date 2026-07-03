-- 데모 데이터 seed (전체 DB 초기화 없이 재실행 가능)
-- 사용: ./back/db/scripts/seed-demo.sh
-- 비율 합계 100%, 출자금 50억(만 원 단위: 500000)

INSERT INTO investors (
    id, name, investment_amount, allocation_ratio, status,
    cumulative_distribution, created_at, created_by
) VALUES
    ('demo-inv-a', '출자자 A', 100000, 20.0, 'active', 0, NOW(), 'seed'),
    ('demo-inv-b', '출자자 B', 150000, 30.0, 'active', 0, NOW(), 'seed'),
    ('demo-inv-c', '출자자 C', 250000, 50.0, 'active', 0, NOW(), 'seed')
ON CONFLICT (name) DO NOTHING;

INSERT INTO investments (
    id, company_name, investment_amount, investment_date, status, created_at, created_by
) VALUES
    ('demo-invest-1', '기업 X', 300000, NOW(), 'active', NOW(), 'seed')
ON CONFLICT (id) DO NOTHING;

INSERT INTO investor_investments (id, investor_id, investment_id, allocated_amount)
SELECT 'demo-ii-a', 'demo-inv-a', 'demo-invest-1', 60000
WHERE EXISTS (SELECT 1 FROM investments WHERE id = 'demo-invest-1')
  AND NOT EXISTS (
      SELECT 1 FROM investor_investments
      WHERE investor_id = 'demo-inv-a' AND investment_id = 'demo-invest-1'
  );

INSERT INTO investor_investments (id, investor_id, investment_id, allocated_amount)
SELECT 'demo-ii-b', 'demo-inv-b', 'demo-invest-1', 90000
WHERE EXISTS (SELECT 1 FROM investments WHERE id = 'demo-invest-1')
  AND NOT EXISTS (
      SELECT 1 FROM investor_investments
      WHERE investor_id = 'demo-inv-b' AND investment_id = 'demo-invest-1'
  );

INSERT INTO investor_investments (id, investor_id, investment_id, allocated_amount)
SELECT 'demo-ii-c', 'demo-inv-c', 'demo-invest-1', 150000
WHERE EXISTS (SELECT 1 FROM investments WHERE id = 'demo-invest-1')
  AND NOT EXISTS (
      SELECT 1 FROM investor_investments
      WHERE investor_id = 'demo-inv-c' AND investment_id = 'demo-invest-1'
  );
