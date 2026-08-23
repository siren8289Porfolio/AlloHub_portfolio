WITH checks AS (
    SELECT
        'INVESTOR_PK_NULL' AS check_name,
        COUNT(*) AS failed_rows
    FROM investors
    WHERE id IS NULL

    UNION ALL
    SELECT
        'INVESTOR_PK_DUPLICATE' AS check_name,
        COUNT(*) AS failed_rows
    FROM (
        SELECT id
        FROM investors
        GROUP BY id
        HAVING COUNT(*) > 1
    ) duplicate_ids

    UNION ALL
    SELECT
        'INVESTMENT_AMOUNT_POSITIVE' AS check_name,
        COUNT(*) AS failed_rows
    FROM investments
    WHERE investment_amount <= 0

    UNION ALL
    SELECT
        'INVESTOR_INVESTMENT_FK_ORPHAN' AS check_name,
        COUNT(*) AS failed_rows
    FROM investor_investments ii
    LEFT JOIN investors i ON i.id = ii.investor_id
    LEFT JOIN investments inv ON inv.id = ii.investment_id
    WHERE i.id IS NULL OR inv.id IS NULL

    UNION ALL
    SELECT
        'INVESTMENT_ALLOCATION_SUM_MISMATCH' AS check_name,
        COUNT(*) AS failed_rows
    FROM (
        SELECT
            inv.id,
            inv.investment_amount,
            COALESCE(SUM(ii.allocated_amount), 0) AS allocated_amount
        FROM investments inv
        LEFT JOIN investor_investments ii ON ii.investment_id = inv.id
        GROUP BY inv.id, inv.investment_amount
    ) allocation_check
    WHERE investment_amount <> allocated_amount

    UNION ALL
    SELECT
        'DISTRIBUTION_AMOUNT_POSITIVE' AS check_name,
        COUNT(*) AS failed_rows
    FROM distributions
    WHERE distribution_amount <= 0

    UNION ALL
    SELECT
        'DISTRIBUTION_DETAIL_FK_ORPHAN' AS check_name,
        COUNT(*) AS failed_rows
    FROM distribution_details dd
    LEFT JOIN distributions d ON d.id = dd.distribution_id
    LEFT JOIN investors i ON i.id = dd.investor_id
    WHERE d.id IS NULL OR i.id IS NULL

    UNION ALL
    SELECT
        'DISTRIBUTION_DETAIL_SUM_MISMATCH' AS check_name,
        COUNT(*) AS failed_rows
    FROM (
        SELECT
            d.id,
            d.distribution_amount,
            COALESCE(SUM(dd.distributed_amount), 0) AS distributed_amount
        FROM distributions d
        LEFT JOIN distribution_details dd ON dd.distribution_id = d.id
        GROUP BY d.id, d.distribution_amount
    ) distribution_check
    WHERE distribution_amount <> distributed_amount

    UNION ALL
    SELECT
        'CASH_BALANCE_NEGATIVE' AS check_name,
        CASE
            WHEN (
                (SELECT COALESCE(SUM(investment_amount), 0) FROM investors)
                - (SELECT COALESCE(SUM(investment_amount), 0) FROM investments)
            ) < 0 THEN 1
            ELSE 0
        END AS failed_rows

    UNION ALL
    SELECT
        'ALLOCATION_RATIO_TOTAL_EXCEEDED' AS check_name,
        CASE
            WHEN (SELECT COALESCE(SUM(allocation_ratio), 0) FROM investors) > 100.0001 THEN 1
            ELSE 0
        END AS failed_rows
)
SELECT
    check_name,
    failed_rows,
    CASE WHEN failed_rows = 0 THEN 'PASS' ELSE 'FAIL' END AS status,
    NOW() AS checked_at
FROM checks
ORDER BY check_name;
