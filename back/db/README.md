# AlloHub — 금융 원장 정합성 및 데이터 파이프라인 설계

> **출자·투자·배분 데이터를 PostgreSQL OLTP 원장으로 관리하고, 금액 불변식 기반 정합성 검증과 Immutable Raw → Validation → Conformed → Finance/Reconciliation Mart 구조를 설계해 금융 데이터의 재현성·추적성·분석 안정성을 확보한 프로젝트**

* **프로젝트 구분:** 개인 프로젝트
* **핵심 역할:** PostgreSQL 원장 설계, Transaction/Constraint 설계, Reconciliation, Data Quality, Analytics Mart 설계, Incremental Ingestion, Lineage 설계
* **기술 스택:** PostgreSQL, Spring Boot, Spring Data JPA, SQL, FastAPI, Docker, OpenAPI
* **외부 데이터:** KVIC, KRX 상장종목정보, OpenDART
* **주요 영역:** DBA / Database Reliability / Data Engineering / Financial Data Quality

---

# 1. 문제 상황 및 요구사항

## 1-1. 프로젝트 배경

AlloHub는 출자자, 기업 투자, 투자금 배분, 배당·회수금 배분을 관리하는 금융 원장형 프로젝트입니다.

이 프로젝트에서 가장 중요한 요구사항은 단순 CRUD가 아니라 **금액 정합성과 변경 추적 가능성**입니다.

핵심 업무 흐름은 다음과 같습니다.

```text
출자자 등록
    ↓
출자금 관리
    ↓
기업 투자 등록
    ↓
출자자별 투자금 자동 배분
    ↓
배당/회수금 등록
    ↓
출자자별 배분액 계산
    ↓
정합성 검증
    ↓
Audit Log
```

프로젝트의 핵심 불변식은 다음 세 가지입니다.

```text
sum(InvestorInvestment.allocated_amount)
= Investment.amount

sum(DistributionDetail.amount)
= Distribution.amount

total_contribution
= total_investment + cash_balance
```

즉 특정 API가 정상적으로 `200 OK`를 반환하는 것보다, **원장의 합계가 항상 맞는 상태로 유지되는 것**이 더 중요합니다.

---

## 1-2. 발생한 문제

### 문제 1. 여러 테이블에 걸친 금융 데이터의 부분 저장 위험

투자 등록 시 하나의 업무가 여러 테이블을 변경합니다.

```text
Investment
    ↓
InvestorInvestment
    ↓
Reconciliation
    ↓
AuditLog
```

이 과정에서 중간 단계가 실패했는데 일부 데이터만 DB에 남으면 원장 전체의 정합성이 깨질 수 있습니다.

배분금 등록 역시:

```text
Distribution
    ↓
DistributionDetail
    ↓
합계 검증
    ↓
원장 확정
```

이 하나의 업무 단위로 처리되어야 합니다.

---

### 문제 2. 애플리케이션 계산 결과와 DB 원장이 달라질 가능성

금융성 금액은 일반 실수 연산처럼 처리하면 반올림이나 소수점 오차로 인해 다음과 같은 문제가 생길 수 있습니다.

```text
Investment.amount
≠
SUM(InvestorInvestment.allocated_amount)
```

또는:

```text
Distribution.amount
≠
SUM(DistributionDetail.amount)
```

이런 문제는 API 한 번의 오류가 아니라 이후 전체 보고·정산 데이터까지 영향을 주는 원장 오류가 됩니다.

---

### 문제 3. 분석을 운영 테이블에서 직접 수행할 경우 책임 혼합

운영 DB는 투자와 배분 transaction을 안전하게 처리하기 위한 구조입니다.

반면 분석은 다음과 같은 질문에 답해야 합니다.

```text
출자자별 투자금은 얼마인가?
기업별 자금 배분은 어떻게 이루어졌는가?
배분 원금과 상세 합계가 일치하는가?
현금잔고와 투자금 총계는 맞는가?
```

운영 Entity를 분석에서 그대로 사용하면 OLTP와 Analytics의 목적이 섞이게 됩니다.

따라서:

```text
Operational PostgreSQL
       ↓
Analytics Data Pipeline
       ↓
Finance / Reconciliation Mart
```

구조를 분리할 필요가 있었습니다.

---

### 문제 4. 데이터 재처리·Backfill 시 중복 위험

동일한 snapshot이나 동일한 extraction 범위를 재실행했을 때 데이터가 중복되면 금융 집계 결과 자체가 변합니다.

따라서 파이프라인 재실행이:

```text
실행 횟수 증가
≠
데이터 중복 증가
```

가 되어야 했습니다.

즉 **idempotent ingestion**이 필요했습니다.

---

### 문제 5. Raw → Mart 변환 과정에서 데이터 오류 전파 가능

잘못된 금액, orphan FK, duplicate PK, 잘못된 배분비율 등이 staging 단계에서 걸러지지 않으면 그대로 Mart까지 전달될 수 있습니다.

특히 금융 데이터에서는:

> 파이프라인 성공 = 데이터가 올바르다

가 아닙니다.

따라서 Business Invariant까지 포함한 DQ가 필요했습니다.

---

# 2. 해결 목표 및 요구사항

## DBA 요구사항

* 투자/배분 Write Path를 하나의 Transaction Boundary로 관리한다.
* 중간 저장 실패 시 전체 Rollback이 가능해야 한다.
* PK/FK/Unique/Check Constraint로 잘못된 데이터를 DB 단계에서도 차단한다.
* 동일 원장 데이터의 중복 반영을 방지한다.
* 금융 금액은 exact numeric 기반으로 관리한다.
* 원장 변경 이력을 Audit Log로 추적할 수 있어야 한다.
* 정합성 검증 결과가 API뿐 아니라 SQL에서도 재현 가능해야 한다.
* Transaction rollback, reconciliation failure, duplicate rejection 등의 운영 지표를 추적 가능한 구조로 만든다.

## DE 요구사항

* 운영 원장을 Analytics Mart로 대체하지 않는다.
* 원본 데이터를 재처리할 수 있도록 Raw Snapshot을 보존한다.
* Raw / Staging / Curated / Mart의 책임을 분리한다.
* Dataset별 Grain을 명확하게 정의한다.
* Pipeline을 재실행해도 데이터가 중복되지 않아야 한다.
* Incremental Extraction을 고려한다.
* Business Invariant 기반 DQ를 수행한다.
* DQ 실패 데이터가 잘못된 Mart로 Publish되지 않아야 한다.
* Schema Version, Source ID, Event Time, Extracted Time을 보존한다.
* `run_id` 기준으로 Raw → Curated → Mart까지 추적 가능해야 한다.

---

# 3. 원인 분석

## 3-1. 원장의 핵심은 테이블 수가 아니라 Transaction Boundary

처음에는 각각의 Entity 저장을 독립적으로 생각할 수 있지만, 금융 원장에서 중요한 것은 어느 테이블에 저장하느냐보다 **어떤 데이터가 하나의 업무 단위로 함께 확정되어야 하느냐**였습니다.

투자 등록 기준으로 분석하면:

```text
Investment만 저장 성공
InvestorInvestment 실패

→ 투자 원금은 존재
→ 출자자별 배분 상세 없음
→ 원장 불일치
```

따라서 다음 전체 흐름을 하나의 Atomic Unit으로 봐야 합니다.

```text
Investment 생성
→ InvestorInvestment 계산
→ 상세 저장
→ 합계 검증
→ AuditLog
```

---

## 3-2. DQ를 기술 규칙이 아니라 Business Invariant에서 도출

일반적인 DQ는 다음 수준에서 끝날 수 있습니다.

```text
NOT NULL
UNIQUE
FK
TYPE
```

하지만 AlloHub에서는 이것만으로 금융 데이터가 맞다고 할 수 없습니다.

예를 들어 모든 PK/FK가 정상이어도:

```text
Investment.amount = 100,000,000

Investor A = 40,000,000
Investor B = 30,000,000
Investor C = 20,000,000

SUM = 90,000,000
```

이면 데이터는 구조적으로 정상이어도 **업무적으로 틀렸습니다.**

따라서 DQ를 원장 규칙과 직접 연결했습니다.

```text
PK/FK Quality
+
Domain Validation
+
Financial Reconciliation
```

---

## 3-3. 운영 DB와 분석 DB의 Grain 차이

운영 Source의 Grain을 먼저 정리했습니다.

| Dataset              | Grain       |
| -------------------- | ----------- |
| `Investor`           | 출자자 1행      |
| `Investment`         | 투자 건 1행     |
| `InvestorInvestment` | 투자 × 출자자 1행 |
| `Distribution`       | 배분 원장 1행    |
| `DistributionDetail` | 배분 × 출자자 1행 |
| `AuditLog`           | 변경 Event 1행 |

이 Grain을 정의하지 않고 JOIN을 시작하면 집계 과정에서 중복 Row가 발생하거나 금액이 배수로 증가할 위험이 있습니다.

따라서 Mart에서도 Fact별 Grain을 분리해야 한다고 판단했습니다.

---

# 4. 문제 해결 및 적용 과정

## Step 1. 금융 원장 Transaction Boundary 설계

투자 등록을 다음 하나의 Transaction으로 정의했습니다.

```text
BEGIN

Investment 생성
    ↓
InvestorInvestment 계산/저장
    ↓
SUM 상세 = Investment.amount 검증
    ↓
AuditLog 기록

COMMIT
```

중간 단계에서 하나라도 실패하면:

```text
ROLLBACK
→ 부분 저장 방지
```

배분 업무도 같은 방식으로:

```text
Distribution
→ DistributionDetail
→ 합계 검증
→ 원장 확정
```

을 하나의 transaction boundary로 처리합니다.

---

## Step 2. Exact Numeric 기반 금융 금액 처리

금융 데이터에는 부동소수점 기반 계산을 피하고 PostgreSQL의 exact numeric/decimal 또는 정수 최소 단위를 사용하는 원칙을 적용했습니다.

또한 반올림 과정에서 생기는 잔여액을 처리하기 위해 계산 순서, 반올림 모드, 마지막 출자자 조정 규칙을 명시적으로 관리하도록 설계했습니다.

핵심은:

```text
계산 결과
→ 저장
```

이 아니라:

```text
계산
→ 반올림 규칙
→ 잔여액 처리
→ 합계 검증
→ 저장
```

순서입니다.

---

## Step 3. DB Constraint + Business Validation 이중 검증

애플리케이션 validation만 믿지 않고 DB의 Constraint와 함께 검증합니다.

주요 검증 항목:

```text
PK
FK
UNIQUE
CHECK
Business Key
금액 > 0
배분비율 범위
합계 정합성
```

잘못된 데이터가 API를 우회해 들어오는 경우에도 DB 단계에서 최소한의 무결성을 유지하는 구조입니다.

---

## Step 4. Reconciliation을 Write Path에 포함

`Reconciliation`을 단순 조회용 Utility가 아니라 원장 확정 과정의 핵심 검증 규칙으로 정의했습니다.

주요 검증식은:

```text
SUM(InvestorInvestment)
=
Investment.amount
```

```text
SUM(DistributionDetail)
=
Distribution.amount
```

```text
total_contribution
=
total_investment + cash_balance
```

입니다.

이 구조를 통해:

```text
Write
→ Reconciliation
→ Valid
→ Commit
```

이라는 흐름을 만들었습니다.

---

## Step 5. Audit Log 기반 변경 추적

투자와 배분의 변경은 다음 정보를 남기도록 설계했습니다.

```text
actor
action
entity
before / after 또는 변경 요약
timestamp
```

금융 원장의 특정 값이 변경되었을 때:

> 현재 값이 무엇인가?

뿐 아니라:

> 누가 언제 어떤 변경을 했는가?

까지 확인할 수 있도록 했습니다.

---

# 5. Data Engineering 해결 과정

## Step 6. Raw → Staging → Curated → Mart 계층 분리

데이터 처리 구조를 다음과 같이 설계했습니다.

```text
PostgreSQL OLTP
       ↓
Immutable Raw / Snapshot
       ↓
Staging Validation
       ↓
Normalized / Conformed
       ↓
Finance & Reconciliation Mart
       ↓
DA / QA / Reporting
```

각 Layer의 역할은 명확히 분리했습니다.

| Layer   | 역할                                     |
| ------- | -------------------------------------- |
| Raw     | Source 원형과 수집 당시 상태 보존                 |
| Staging | Type, NULL, Duplicate, FK 검증           |
| Curated | Investor/Investment/Distribution 관계 통합 |
| Mart    | 분석 및 정합성 확인 목적 데이터 제공                  |

운영 원장은 계속 PostgreSQL OLTP가 Write Authority를 갖고, Mart는 읽기/분석용으로만 사용합니다.

---

## Step 7. Immutable Snapshot으로 재현성 확보

Raw Layer에서는 수집 당시의 원본을 가능한 그대로 보존하고 다음 Metadata를 유지하도록 했습니다.

```text
source_id
source_pk
business_date / event_time
extracted_at
schema_version
```

이를 통해 특정 분석 결과에 대해:

```text
어느 Source 데이터였는가?
어느 시점에 수집했는가?
어떤 Schema였는가?
```

를 역추적할 수 있는 구조를 마련했습니다.

---

## Step 8. Incremental Ingestion + Idempotency 설계

MVP에서는 Batch Snapshot 또는 증분 추출 방식으로 시작하도록 했습니다.

증분 기준은:

```text
updated_at
또는
monotonic key
```

를 사용합니다.

재실행 시 동일 데이터가 중복 생성되지 않도록:

```text
source
+
source_key
+
reference_date
```

등 안정적인 식별자를 기준으로 idempotent 적재를 적용했습니다.

Backfill 시에도 이미 확정된 결과를 조용히 덮어쓰지 않고 snapshot과 처리 구간을 고정하는 방향으로 설계했습니다.

---

## Step 9. Business Invariant 기반 Data Quality

실제 구현 가능한 DQ SQL은:

```text
back/db/analytics/01_quality_checks.sql
```

에 정리되어 있습니다.

검증 내용은:

```text
PK NULL / Duplicate
FK Orphan
음수 금액
투자 상세 합계
배분 상세 합계
현금 잔고
배분 비율 합계
```

입니다.

특히 중요한 점은 다음입니다.

### 일반 DQ

```text
PK NULL = 0
FK orphan = 0
amount < 0 = 0
```

### 금융 DQ

```text
SUM(InvestorInvestment)
= Investment.amount

SUM(DistributionDetail)
= Distribution.amount

Contribution
= Investment + Cash
```

즉 **Schema Quality와 Business Quality를 함께 검증**합니다.

---

## Step 10. DQ Failure → Publish 차단

DQ 실패를 단순 Warning으로 기록하고 Mart를 계속 생성하면 잘못된 데이터가 분석가에게 전달됩니다.

따라서:

```text
Extract
   ↓
Validation
   ↓
DQ FAIL
 ├→ Quarantine
 └→ Pipeline Fail
```

로 처리하고,

```text
DQ PASS
→ Transform
→ Publish
```

하도록 설계했습니다.

---

# 6. Analytics Mart 설계

운영 Entity를 그대로 노출하지 않고 분석 Grain에 맞는 Fact/Dimension을 제공합니다.

현재 실행 가능한 SQL에는 다음 View가 구성되어 있습니다.

```text
analytics.dim_investor
analytics.dim_company
analytics.dim_date

analytics.fact_investment_allocation
analytics.fact_distribution_allocation
analytics.fact_reconciliation_check
```

## Fact 1. Investment Allocation

```text
Investment
     ↓
InvestorInvestment
     ↓
fact_investment_allocation
```

분석 가능 항목:

```text
투자 건별 출자자 배분
기업별 투자금
출자자별 투자 참여 금액
```

## Fact 2. Distribution Allocation

```text
Distribution
      ↓
DistributionDetail
      ↓
fact_distribution_allocation
```

분석 가능 항목:

```text
배당/회수 건별 분배
출자자별 배분 금액
기업/기간별 회수 흐름
```

## Fact 3. Reconciliation Check

```text
Operational Ledger
      ↓
Reconciliation
      ↓
fact_reconciliation_check
```

이를 통해 데이터 소비자가 운영 Table의 복잡한 관계를 매번 직접 해석하지 않고 **원장 정합성 자체를 분석 데이터로 확인**할 수 있습니다.

---

# 7. 외부 Reference Data Pipeline

AlloHub는 내부 원장에 외부 기업/펀드 Reference 데이터를 결합할 수 있도록 별도 Pipeline Boundary를 설계했습니다.

주요 Source:

```text
KVIC
→ Fund / GP Reference

KRX 상장종목정보
→ Company Master

OpenDART
→ 기업개황 / 공시 / 재무 Reference
```

처리 흐름은:

```text
Extract
   ↓
Raw Snapshot
   ↓
Schema Validation
   ↓
Normalize
   ↓
Identity Matching
   ↓
Reference Tables
   ↓
Serving
```

입니다.

---

## Step 11. 기업 식별자 Matching

KRX와 DART의 기업 데이터를 단순 기업명 String Join으로 연결하지 않고:

```text
법인등록번호
종목코드
기타 Stable Identifier
```

처럼 가능한 식별자를 우선 사용하도록 설계했습니다.

매칭되지 않는 데이터는 억지로 결합하지 않고 DQ 대상으로 격리합니다.

---

## Step 12. 외부 데이터 장애를 내부 원장과 격리

외부 KRX/DART/KVIC 수집이 실패했다고 해서 출자·투자·배분 원장이 멈추면 안 됩니다.

따라서:

```text
External Reference Failure
        ↓
Reference Freshness = DEGRADED

Internal Ledger
        ↓
정상 처리
```

로 경계를 분리했습니다.

이는 DE와 DBA 모두에서 중요한 **Failure Domain 분리**입니다.

---

# 8. Schema Change / Contract 관리

데이터 Pipeline에서는 Column 변경도 중요한 장애 원인입니다.

변경을 다음처럼 분류합니다.

```text
Column Add
Column Drop
Type Change
```

그리고:

```text
Producer Change
     ↓
Staging Validation
     ↓
Consumer Compatibility
     ↓
Downstream Test
     ↓
Publish
```

순서로 검증합니다.

모든 데이터에는 가능한 범위에서:

```text
schema_version
migration_id
```

를 남겨 Schema Evolution을 추적하도록 했습니다.

---

# 9. Pipeline 운영 및 Monitoring

Airflow를 도입하는 경우 DAG는 다음 Failure Domain으로 분리하도록 설계했습니다.

```text
Extract
  ↓
Validate
  ↓
Transform
  ↓
Publish
```

그리고 Run 단위로 다음 Metadata를 남깁니다.

```text
run_id
logical/data interval
source snapshot
code version
schema version
row count
DQ status
retry count
started_at
finished_at
```

모니터링 대상은 다음과 같습니다.

```text
Pipeline Success Rate
Data Freshness
Row-count Anomaly
DQ Failure
Reconciliation Mismatch
Late Data
Schema Drift
```

장애 발생 시 `run_id`를 기준으로:

```text
Raw
 ↓
Staging
 ↓
Curated
 ↓
Mart
```

까지 역추적할 수 있도록 설계했습니다.

---

# 10. 해결 결과 및 성과

AlloHub는 pivotSeoul처럼 latency를 몇 % 줄였다는 성능 benchmark 프로젝트는 아닙니다.

Repository에서도 `p95 1초`, `계산 500ms`, `가용성 99.5%`는 **검증 전 목표값**이며 부하테스트나 DB Plan 근거 없이는 달성했다고 표현하지 않도록 명시하고 있습니다.

따라서 이 프로젝트의 성과는 **정합성·재현성·구조적 안정성**을 중심으로 표현합니다.

| 개선 영역     | Before 위험          | After                         |
| --------- | ------------------ | ----------------------------- |
| 투자 저장     | 여러 저장 작업의 부분 성공 가능 | Transaction 단위 Atomic 처리      |
| 투자 배분     | 상세 합계 불일치 가능       | 원금 = 상세 합계 Reconciliation     |
| 배분금       | 원금과 상세 배분 불일치 가능   | 배분 상세 합계 검증                   |
| 데이터 품질    | PK/FK 위주 검사        | 금융 Business Invariant DQ 추가   |
| 분석        | 운영 Entity 직접 조회    | Fact / Dimension Mart 분리      |
| 재처리       | 동일 데이터 중복 가능       | Idempotent Ingestion          |
| 이력        | 현재값 중심             | Audit Log + Snapshot          |
| Schema    | 변경 영향 추적 어려움       | schema_version / migration 기준 |
| 장애 분석     | 어느 단계에서 깨졌는지 불명확   | run_id 기반 lineage             |
| 외부 데이터 장애 | 내부 서비스 영향 가능       | Reference Pipeline과 Ledger 격리 |

---

# 11. DBA 관점 핵심 성과

## ① Transaction & Consistency

```text
Multiple Table Write
→ Transaction Boundary
→ Reconciliation
→ Commit / Rollback
```

## ② Data Integrity

```text
PK / FK / UNIQUE / CHECK
+
Business Validation
+
Financial Reconciliation
```

## ③ Exact Numeric

```text
Financial Amount
→ exact numeric
→ explicit rounding
→ residual adjustment
→ sum validation
```

## ④ Auditability

```text
actor
action
entity
before/after
timestamp
```

## ⑤ Failure Isolation

```text
External Reference Pipeline Failure
≠
Internal Financial Ledger Failure
```

---

# 12. DE 관점 핵심 성과

핵심 데이터 흐름은 다음과 같습니다.

```text
PostgreSQL OLTP
       ↓
Immutable Raw Snapshot
       ↓
Validation / DQ
       ↓
Normalized / Conformed
       ↓
Finance & Reconciliation Mart
       ↓
DA / QA / Reporting
```

외부 Reference까지 포함하면:

```text
KVIC / KRX / OpenDART
          ↓
       Extract
          ↓
   Immutable Raw
          ↓
 Schema Validation
          ↓
      Normalize
          ↓
 Identity Matching
          ↓
 Reference Tables
          ↓
 Internal Ledger / Mart
```

그리고 운영 핵심은:

```text
Idempotency
Reconciliation
Lineage
Schema Version
Data Quality
Failure Isolation
```

입니다.

---

# 13. 검증 방법

현재 Repository에서 실행 가능한 핵심 DE 산출물은 다음과 같습니다.

```text
back/db/analytics/
├── 01_quality_checks.sql
├── 02_analytics_marts.sql
└── README.md
```

### 검증 1. Data Quality

```text
PK duplicate
FK orphan
negative amount
allocation sum
distribution sum
cash balance
ratio sum
```

### 검증 2. Reconciliation

```text
Investment.amount
↔ InvestorInvestment SUM
```

```text
Distribution.amount
↔ DistributionDetail SUM
```

### 검증 3. Analytics Mart

```text
Operational PostgreSQL
→ analytics.dim_*
→ analytics.fact_*
```

### 검증 4. External Reference

```text
Duplicate Company
Identifier NULL
KRX/DART Match
Stale Reference
Duplicate Corporate Event
Raw ↔ Normalized Row Count
```

---

# 14. 회고 및 배운 점

첫째, 금융 데이터 프로젝트에서는 성능보다 먼저 **정합성의 기준을 명시해야 한다는 점**을 배웠습니다.

`NOT NULL`이나 `FK`가 모두 정상이어도 투자금 상세 합계가 원금과 다르면 그 데이터는 사용할 수 없습니다. 그래서 Data Quality를 DB 기술 규칙뿐 아니라 업무 불변식에서 도출해야 했습니다.

둘째, Transaction은 단순히 메서드에 `@Transactional`을 붙이는 문제가 아니라 **업무에서 어디까지가 하나의 원자적 변경인지 정의하는 과정**이라는 점을 확인했습니다.

셋째, 데이터 엔지니어링에서 Raw 데이터 보존은 단순 백업이 아니라 재현성을 위한 기반이라는 점을 배웠습니다. 특정 Mart 결과가 이상할 때 원본 Snapshot과 Schema Version까지 되돌아갈 수 있어야 원인을 분석할 수 있습니다.

넷째, 운영 DB와 Analytics Mart를 분리하면서 같은 데이터를 서로 다른 Grain과 목적에서 바라봐야 한다는 점을 이해했습니다. 운영 구조가 분석에 최적이라고 가정해서는 안 되고, 분석 소비자에게는 명확한 Fact/Dimension과 Metric 기준을 제공해야 합니다.

다섯째, Pipeline의 안정성은 “한 번 성공적으로 실행된다”가 아니라 **retry, backfill, duplicate 방지, schema change, DQ failure 상황에서도 결과가 예측 가능해야 한다는 것**이라고 배웠습니다.

---

# 15. 현재 프로젝트에서 주장하지 않는 것

현재 Repository가 근거를 제공하지 않는 아래 항목은 포트폴리오 성과로 주장하지 않습니다.

* PostgreSQL Query Latency 몇 % 개선
* `EXPLAIN ANALYZE` 기반 인덱스 튜닝 실측
* AWS RDS / Aurora 운영
* Redis Cluster / Sentinel
* PgBouncer
* HA / Primary-Replica Failover
* RTO / RPO 측정
* Backup / Restore 자동화
* DB CPU / Connection 사용률 개선
* Kafka / Debezium CDC
* Airflow 실제 운영 배포
* 1,000만 건 이상 Load Test
* SRS 목표치인 p95 1초 / 계산 500ms / 99.5% 가용성 달성

현재 README도 이 성능·가용성 값은 **검증 전 목표값이며 evidence 없이는 성과로 표현하지 않는다**고 명시합니다.

---

# 16. 포트폴리오용 최종 설명

> **AlloHub에서 출자·투자·배분 데이터를 관리하는 PostgreSQL 금융 원장과 분석 데이터 파이프라인을 설계했습니다. 투자 등록 시 `Investment → InvestorInvestment → Reconciliation → AuditLog`, 배분 등록 시 `Distribution → DistributionDetail → 합계 검증`을 하나의 Transaction Boundary로 정의해 부분 저장을 방지하고, 투자금과 상세 배분액·배분 원금과 상세 금액·총 출자금과 투자금/현금 간 Business Invariant를 정합성 검증 규칙으로 적용했습니다. Data Engineering 영역에서는 운영 PostgreSQL을 Write Authority로 유지하면서 `Immutable Raw → Validation → Conformed → Finance/Reconciliation Mart`로 계층을 분리했습니다. PK/FK뿐 아니라 원장 합계까지 검증하는 Data Quality SQL과 `dim_investor`, `dim_company`, `dim_date`, `fact_investment_allocation`, `fact_distribution_allocation`, `fact_reconciliation_check` 분석 Mart를 구성했습니다. 또한 `updated_at` 또는 안정적인 Key 기반 증분 추출, idempotent ingestion, schema version, run_id 기반 lineage를 설계하고 KVIC·KRX·OpenDART 외부 Reference Pipeline을 내부 금융 원장과 장애 격리했습니다.**

## 한 줄 성과

> **PostgreSQL 금융 원장을 Transaction·Reconciliation·Audit 중심으로 설계하고, Immutable Snapshot·Business Invariant DQ·Idempotent Ingestion·Finance Mart를 연결해 금융 데이터의 정합성과 재현성을 중심으로 Data Engineering 구조를 구축했습니다.**
