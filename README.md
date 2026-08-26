# AlloHub DA Branch README

이 브랜치의 README는 **DA(Data Analytics)** 범위만 다룬다.

## 1. 목적

DA의 목적은 운영 원장을 기준으로 투자/배분 상태를 분석하고, 정합성 리스크를 빠르게 식별할 수 있는 분석 지표와 조회 뷰를 제공하는 것이다.

## 2. 분석 대상 데이터

- `Investor`
- `Investment`
- `InvestorInvestment`
- `Distribution`
- `DistributionDetail`
- `AuditLog`

## 3. 분석 마트(뷰)

- `analytics.dim_investor`
- `analytics.dim_company`
- `analytics.dim_date`
- `analytics.fact_investment_allocation`
- `analytics.fact_distribution_allocation`
- `analytics.fact_reconciliation_check`

## 4. 핵심 지표

| 지표 | 정의 |
| --- | --- |
| Allocation Coverage | `allocated_amount / investment_amount` |
| Distribution Execution Rate | `distribution_amount / allocated_amount` |
| Reconciliation Pass Rate | `PASS 건수 / 전체 점검 건수` |
| Residual Cash | `investment_amount - sum(distribution_detail)` |
| Concentration Ratio | 상위 N 출자자 금액 / 전체 금액 |

## 5. 운영 기준

- DQ 결과가 `ALL PASS`일 때만 분석 결과 publish
- 모든 수치는 원장 키(`investment_id`, `distribution_id`)로 drill-down 가능해야 함
- 분석 레이어는 운영 원장 write authority를 대체하지 않음

## 6. 실행 SQL

```bash
psql "$DATABASE_URL" -f back/db/analytics/01_quality_checks.sql
psql "$DATABASE_URL" -f back/db/analytics/02_analytics_marts.sql
```

## 7. 예시 쿼리

```sql
SELECT
  d.year,
  d.month,
  SUM(fia.investment_amount) AS investment_amount,
  SUM(fda.distribution_amount) AS distribution_amount,
  SUM(fia.investment_amount) - SUM(fda.distribution_amount) AS residual_amount
FROM analytics.fact_investment_allocation fia
JOIN analytics.dim_date d ON d.date_key = fia.date_key
LEFT JOIN analytics.fact_distribution_allocation fda
  ON fda.investment_id = fia.investment_id
 AND fda.date_key = fia.date_key
GROUP BY d.year, d.month
ORDER BY d.year, d.month;
```
