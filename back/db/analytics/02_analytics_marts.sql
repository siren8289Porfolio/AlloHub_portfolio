CREATE SCHEMA IF NOT EXISTS analytics;

CREATE OR REPLACE VIEW analytics.dim_investor AS
SELECT
    id AS investor_id,
    name AS investor_name,
    investment_amount AS contribution_amount,
    allocation_ratio,
    status,
    cumulative_distribution,
    created_at,
    updated_at
FROM investors;

CREATE OR REPLACE VIEW analytics.dim_company AS
SELECT
    LOWER(TRIM(company_name)) AS company_key,
    company_name,
    COUNT(*) AS investment_count,
    MIN(investment_date) AS first_investment_at,
    MAX(investment_date) AS latest_investment_at
FROM investments
GROUP BY LOWER(TRIM(company_name)), company_name;

CREATE OR REPLACE VIEW analytics.dim_date AS
SELECT DISTINCT
    DATE(investment_date) AS date_id,
    EXTRACT(YEAR FROM investment_date)::INTEGER AS year,
    EXTRACT(MONTH FROM investment_date)::INTEGER AS month,
    EXTRACT(DAY FROM investment_date)::INTEGER AS day
FROM investments
UNION
SELECT DISTINCT
    DATE(distribution_date) AS date_id,
    EXTRACT(YEAR FROM distribution_date)::INTEGER AS year,
    EXTRACT(MONTH FROM distribution_date)::INTEGER AS month,
    EXTRACT(DAY FROM distribution_date)::INTEGER AS day
FROM distributions;

CREATE OR REPLACE VIEW analytics.fact_investment_allocation AS
SELECT
    inv.id AS investment_id,
    inv.company_name,
    inv.investment_date AS event_time,
    DATE(inv.investment_date) AS business_date,
    inv.investment_amount,
    ii.id AS investor_investment_id,
    ii.investor_id,
    i.name AS investor_name,
    i.allocation_ratio,
    ii.allocated_amount,
    inv.status AS investment_status,
    NOW() AS extracted_at,
    'v1' AS schema_version
FROM investments inv
JOIN investor_investments ii ON ii.investment_id = inv.id
JOIN investors i ON i.id = ii.investor_id;

CREATE OR REPLACE VIEW analytics.fact_distribution_allocation AS
SELECT
    d.id AS distribution_id,
    d.investment_id,
    inv.company_name,
    d.distribution_date AS event_time,
    DATE(d.distribution_date) AS business_date,
    d.distribution_type,
    d.distribution_amount,
    dd.id AS distribution_detail_id,
    dd.investor_id,
    i.name AS investor_name,
    dd.distributed_amount,
    d.status AS distribution_status,
    NOW() AS extracted_at,
    'v1' AS schema_version
FROM distributions d
JOIN investments inv ON inv.id = d.investment_id
JOIN distribution_details dd ON dd.distribution_id = d.id
JOIN investors i ON i.id = dd.investor_id;

CREATE OR REPLACE VIEW analytics.fact_reconciliation_check AS
SELECT
    'INVESTMENT_ALLOCATION_SUM' AS check_name,
    inv.id AS source_id,
    inv.investment_date AS event_time,
    inv.investment_amount AS expected_amount,
    COALESCE(SUM(ii.allocated_amount), 0) AS actual_amount,
    CASE
        WHEN inv.investment_amount = COALESCE(SUM(ii.allocated_amount), 0) THEN 'PASS'
        ELSE 'FAIL'
    END AS status,
    NOW() AS checked_at,
    'v1' AS schema_version
FROM investments inv
LEFT JOIN investor_investments ii ON ii.investment_id = inv.id
GROUP BY inv.id, inv.investment_date, inv.investment_amount

UNION ALL

SELECT
    'DISTRIBUTION_DETAIL_SUM' AS check_name,
    d.id AS source_id,
    d.distribution_date AS event_time,
    d.distribution_amount AS expected_amount,
    COALESCE(SUM(dd.distributed_amount), 0) AS actual_amount,
    CASE
        WHEN d.distribution_amount = COALESCE(SUM(dd.distributed_amount), 0) THEN 'PASS'
        ELSE 'FAIL'
    END AS status,
    NOW() AS checked_at,
    'v1' AS schema_version
FROM distributions d
LEFT JOIN distribution_details dd ON dd.distribution_id = d.id
GROUP BY d.id, d.distribution_date, d.distribution_amount

UNION ALL

SELECT
    'CASH_BALANCE_NON_NEGATIVE' AS check_name,
    'ledger' AS source_id,
    NOW() AS event_time,
    0 AS expected_amount,
    (
        (SELECT COALESCE(SUM(investment_amount), 0) FROM investors)
        - (SELECT COALESCE(SUM(investment_amount), 0) FROM investments)
    ) AS actual_amount,
    CASE
        WHEN (
            (SELECT COALESCE(SUM(investment_amount), 0) FROM investors)
            - (SELECT COALESCE(SUM(investment_amount), 0) FROM investments)
        ) >= 0 THEN 'PASS'
        ELSE 'FAIL'
    END AS status,
    NOW() AS checked_at,
    'v1' AS schema_version;
