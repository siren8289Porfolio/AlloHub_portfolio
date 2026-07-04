# AlloHub 

## 1. 핵심 한 줄

> **AlloHub는 투자/분배 목록 조회에서 DB를 여러 번 왕복하던 N+1 구조를 fetch join으로 줄이고, 감사 로그 최신순 조회에 맞춘 인덱스를 추가해 조회 경로를 최적화한 프로젝트입니다.**

---

## 2. 효율화 배경

AlloHub는 JPA/Hibernate 기반 백엔드입니다. JPA는 객체 중심으로 데이터를 다룰 수 있게 해주지만, 연관 엔티티를 언제 가져오는지 잘못 설계하면 목록 조회에서 DB를 반복 호출하는 문제가 생길 수 있습니다.

Hibernate 공식문서에서도 데이터를 언제, 어떻게 가져오는지(fetching)를 조정하는 것은 애플리케이션 성능에 큰 영향을 주는 요소라고 설명합니다. 특히 `SELECT` fetching은 별도 SQL select로 연관 데이터를 가져오는 방식이며, 일반적으로 N+1 select라고 불리는 전략이라고 설명합니다. 반대로 `JOIN` fetching은 SQL outer join을 사용해 연관 데이터를 함께 가져오는 방식입니다.

비전공자 관점에서 보면, 기존 구조는 이런 상황에 가깝습니다.

```text id="b72i0v"
투자 목록을 가져온다.
→ 1번 투자자의 정보를 다시 찾는다.
→ 2번 투자자의 정보를 다시 찾는다.
→ 3번 투자자의 정보를 다시 찾는다.
→ ...
→ 투자 건수가 늘어날수록 DB 조회도 계속 늘어난다.
```

즉, 목록을 한 번 가져온 뒤 각 항목의 관련 정보를 다시 물어보는 구조였습니다.

---

## 3. 코드 효율화: N+1 쿼리 제거

## 3-1. 기존 문제

`InvestmentService.list()`와 `DistributionService.list()`는 목록 데이터를 가져올 때 먼저 `findAll()`로 기본 목록을 조회한 뒤, 각 항목마다 연결된 투자자나 투자 정보를 다시 꺼내는 구조였습니다.

```text id="asb0rr"
Before

1. findAll()로 기본 목록 조회
2. 각 Investment마다 InvestorInvestment 조회
3. 각 InvestorInvestment마다 Investor 조회
4. 각 Distribution마다 Investment 조회
5. 데이터가 늘수록 추가 SQL 증가
```

이런 문제를 보통 **N+1 쿼리 문제**라고 부릅니다. 처음 목록 조회 1번에, 목록 개수 N개만큼 추가 조회가 붙을 수 있는 구조입니다.

비전공자식으로 말하면,
장부 전체를 한 번에 가져오지 않고 **“목록 한 장 받고 → 관련 정보 한 명씩 다시 물어보는 구조”**입니다.

---

## 3-2. 개선 방법

목록 화면에서 실제로 필요한 연관 데이터를 Repository 단계에서 미리 같이 가져오도록 변경했습니다.

| 변경 파일                         | 개선 내용                                                                     |
| ----------------------------- | ------------------------------------------------------------------------- |
| `InvestmentRepository.java`   | 투자 목록을 가져올 때 `investorInvestments`, `investor`까지 `LEFT JOIN FETCH`로 함께 조회 |
| `InvestmentService.java`      | 기존 `findAll()` 대신 `findAllWithInvestorInvestments()` 사용                   |
| `DistributionRepository.java` | 분배 목록을 가져올 때 연결된 `investment`까지 `LEFT JOIN FETCH`로 함께 조회                  |
| `DistributionService.java`    | 기존 `findAll()` 대신 `findAllWithInvestment()` 사용                            |

개선 후 흐름은 아래와 같습니다.

```text id="l3uhp2"
After

1. 투자 목록 + 투자자 정보를 한 번에 조회
2. 분배 목록 + 투자 정보를 한 번에 조회
3. Service에서는 이미 가져온 데이터를 조립
4. 항목마다 DB를 다시 조회하지 않음
```

비전공자식으로 말하면,
기존에는 손님 명단을 받은 뒤 한 명씩 주소를 다시 물어봤다면, 개선 후에는 **처음부터 이름·주소가 같이 적힌 명단을 받는 방식**입니다.

---

## 3-3. 왜 효율화인가?

기존 구조는 데이터가 5건일 때는 크게 티가 나지 않아도, 100건, 1,000건으로 늘어나면 DB에 계속 추가 요청을 보내는 구조입니다.

```text id="l2r7if"
기존 구조:
목록 1번 조회
+ 각 행마다 추가 조회
→ 데이터가 늘수록 DB 왕복 증가

개선 구조:
목록과 필요한 연관 데이터를 함께 조회
→ DB 왕복 횟수 감소
→ 목록 조회 안정성 증가
```

그래서 이 작업은 단순히 Repository 메서드 이름을 바꾼 것이 아니라, **DB 접근 횟수를 줄인 조회 성능 개선**입니다.

---

## 4. 성능 회귀 테스트 추가

성능 개선은 “고쳤다”에서 끝내면 나중에 같은 문제가 다시 생길 수 있습니다.
그래서 Hibernate `Statistics`를 사용해 `list()` 실행 시 실제 SQL이 몇 번 실행되는지 테스트했습니다.

Hibernate `Statistics` 공식 Javadoc은 `hibernate.generate_statistics=true`로 통계 수집을 켤 수 있고, `Statistics`가 SessionFactory에 속한 모든 세션의 통계를 노출한다고 설명합니다.

| 테스트 파일                              | 검증 내용                                  |
| ----------------------------------- | -------------------------------------- |
| `InvestmentServiceQueryCountTest`   | 투자 목록 조회 시 SQL 실행 횟수가 과도하게 늘어나지 않는지 확인 |
| `DistributionServiceQueryCountTest` | 분배 목록 조회 시 SQL 실행 횟수가 과도하게 늘어나지 않는지 확인 |

검증 방식은 다음과 같습니다.

```text id="0c0pmm"
투자/분배 데이터 5건 생성
→ list() 실행
→ SQL 실행 횟수가 2개 이하인지 확인
```

기존 `findAll()` 방식으로 되돌리면 데이터 건수만큼 추가 쿼리가 발생해 테스트가 실패합니다. 즉, 나중에 누군가 실수로 다시 N+1 구조를 만들면 테스트 단계에서 바로 잡을 수 있습니다.

비전공자식으로 말하면,
단순히 “문제를 고쳤다”가 아니라 **같은 문제가 다시 생기면 자동으로 알람이 울리는 검사표를 추가한 것**입니다.

---

## 5. DB 효율화: 감사 로그 최신순 인덱스 추가

## 5-1. 기존 문제

`AuditLogRepository.findTop100ByOrderByCreatedAtDesc()`는 감사 로그를 최신순으로 100개만 가져오는 조회입니다.

```text id="qotliy"
감사 로그 전체 중에서
가장 최근에 쌓인 100개만 보여줘.
```

그런데 `created_at` 기준 인덱스가 없으면 로그가 많아질수록 DB가 전체 로그를 훑고, 다시 최신순으로 정렬한 뒤, 상위 100개를 잘라야 할 수 있습니다.

```text id="6s4k8a"
Before

전체 로그 읽기
→ created_at 기준으로 정렬
→ 상위 100개 선택
```

비전공자식으로 말하면,
가장 최근 기록 100개를 찾기 위해 **전체 일지를 처음부터 끝까지 뒤진 뒤 다시 날짜순으로 정렬하는 방식**입니다.

---

## 5-2. 개선 방법

`audit_logs.created_at` 기준으로 내림차순 인덱스를 추가했습니다.

```java id="yt8v7b"
@Table(
    name = "audit_logs",
    indexes = @Index(
        name = "idx_audit_logs_created_at",
        columnList = "created_at DESC"
    )
)
public class AuditLog {
}
```

운영 DB에 직접 반영해야 하는 SQL은 다음과 같습니다.

```sql id="kkvmv1"
CREATE INDEX idx_audit_logs_created_at
ON audit_logs (created_at DESC);
```

PostgreSQL 공식문서는 `ORDER BY`와 `LIMIT n`이 함께 있는 경우, 정렬 조건에 맞는 인덱스가 있으면 인덱스 순서대로 앞의 n개 행을 바로 가져올 수 있다고 설명합니다. 또한 B-tree 인덱스는 `ASC`, `DESC`, `NULLS FIRST`, `NULLS LAST` 같은 정렬 옵션을 지정해 만들 수 있습니다.

즉, 이번 인덱스는 단순히 “인덱스를 하나 더 만든 것”이 아니라, **최신 감사 로그 100개 조회 패턴에 맞춰 책갈피를 꽂은 작업**입니다.

---

## 5-3. 인덱스 추가 효과

변경 전후를 비교하면 아래와 같습니다.

```text id="jb5xro"
Before

audit_logs 전체 확인
→ created_at 기준 정렬
→ 최신 100개 선택

After

created_at DESC 인덱스 활용
→ 최신순으로 정리된 길을 따라감
→ 필요한 100개만 선택
```

| 항목           | 개선 효과                        |
| ------------ | ---------------------------- |
| 최신 로그 조회     | 전체 정렬 부담 감소                  |
| 로그 데이터 증가 대응 | 로그가 쌓여도 조회 경로 안정화            |
| 관리자/감사 화면    | 최신 100건 확인 속도 개선             |
| DB 부하        | 불필요한 Full Scan + Sort 가능성 감소 |

비전공자식으로 말하면,
전체 일지를 매번 뒤지는 것이 아니라 **이미 최신순으로 정리된 색인표에서 앞 100개만 보는 방식**입니다.

---

## 6. 운영 반영 시 주의점

AlloHub는 Flyway 같은 마이그레이션 도구 없이 `ddl-auto`로 스키마를 관리하고 있습니다. 따라서 환경별 동작을 구분해야 합니다.

| 환경    | `ddl-auto`    | 의미                        |
| ----- | ------------- | ------------------------- |
| local | `update`      | Entity 변경을 DB에 반영 가능      |
| test  | `create-drop` | 테스트마다 스키마 생성 후 삭제         |
| prod  | `validate`    | 스키마가 맞는지만 확인하고 직접 변경하지 않음 |

Spring Boot 공식문서는 `spring.jpa.hibernate.ddl-auto` 값을 명시할 수 있고, 표준 Hibernate 값으로 `none`, `validate`, `update`, `create`, `create-drop` 등을 사용할 수 있다고 설명합니다. 또한 embedded DB일 때는 기본값이 `create-drop`, 그 외에는 `none`이 될 수 있다고 안내합니다.

따라서 로컬과 테스트 환경에서는 인덱스가 자동으로 반영될 수 있지만, 운영 환경이 `validate`라면 운영 DB에는 자동 생성되지 않습니다.

운영 반영이 필요하면 PostgreSQL에 아래 SQL을 별도로 실행해야 합니다.

```sql id="b8hkr0"
CREATE INDEX idx_audit_logs_created_at
ON audit_logs (created_at DESC);
```

---

## 7. 이미 잘 되어 있던 부분

이번에 새로 고치지 않아도 되는 부분도 있었습니다.

| 항목                                        | 의미                                 |
| ----------------------------------------- | ---------------------------------- |
| 생성자 기반 DI                                 | 객체 의존성을 명확하게 주입하는 구조               |
| `GlobalExceptionHandler` / `AppException` | 예외 처리 방식 일원화                       |
| `@Transactional(readOnly = true)`         | 조회 전용 트랜잭션 구분                      |
| `spring.jpa.open-in-view: false`          | 화면 응답 단계에서 무분별한 지연 로딩이 발생하지 않도록 제한 |

즉, AlloHub 백엔드는 기본적인 계층 구조와 예외 처리, 조회 트랜잭션 구분은 이미 잡혀 있었고, 이번 작업은 그중 **목록 조회 성능 병목 가능성이 있는 부분을 보완한 것**입니다.

---

## 8. 최종적으로 줄어든 비용

이번 코드·DB 효율화로 줄어든 비용은 아래와 같습니다.

```text id="5gvzry"
1. 목록 조회 시 DB 왕복 횟수 감소
2. 투자/분배 데이터 증가에 따른 추가 쿼리 위험 감소
3. N+1 문제가 재발했을 때 테스트 단계에서 감지 가능
4. 최신 감사 로그 조회 시 정렬 부담 감소
5. 운영 DB 반영 방식 명확화
6. 조회 성능 개선 근거를 공식문서와 테스트로 설명 가능
```

---

## 9. 문서용 요약 문장

AlloHub 코드·DB 효율화는 JPA/Hibernate 기반 조회 구조에서 발생할 수 있는 N+1 쿼리 문제와 최신 감사 로그 조회용 인덱스 부재를 개선한 작업입니다.

기존에는 `InvestmentService.list()`와 `DistributionService.list()`가 `findAll()`로 기본 목록을 조회한 뒤, 각 항목의 연관 엔티티를 지연 로딩하면서 데이터 건수만큼 추가 쿼리가 발생할 수 있었습니다. 이를 Repository 단계에서 `LEFT JOIN FETCH`로 필요한 연관 데이터를 한 번에 가져오도록 변경했습니다.

또한 Hibernate `Statistics` 기반 회귀 테스트를 추가해, 향후 동일한 N+1 문제가 다시 생기면 테스트 단계에서 감지되도록 했습니다. 테스트는 투자/분배 데이터 5건 기준으로 `list()` 실행 시 SQL 개수가 2개 이하인지 확인합니다.

추가로 `AuditLogRepository.findTop100ByOrderByCreatedAtDesc()` 조회 패턴에 맞춰 `audit_logs.created_at DESC` 인덱스를 추가했습니다. 이 인덱스는 최신 감사 로그 100건 조회 시 전체 테이블을 읽고 정렬하는 부담을 줄이기 위한 것입니다.

다만 AlloHub 운영 환경은 `ddl-auto: validate`라 Entity의 인덱스 설정이 운영 DB에 자동 반영되지 않습니다. 따라서 실제 운영 PostgreSQL에는 `CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);` SQL을 별도로 실행해야 합니다.

---

## 10. 한 줄 설명

> **AlloHub의 투자/분배 목록 조회에서 DB를 여러 번 호출하던 N+1 구조를 fetch join으로 한 번에 조회하도록 바꾸고, Hibernate Statistics 테스트와 감사 로그 최신순 인덱스로 재발 방지와 조회 경로 최적화까지 적용한 백엔드 효율화 작업입니다.**

---

## 11. README 카드용 문장

> **투자/분배 목록 조회에서 N+1 쿼리 가능성을 제거하기 위해 `LEFT JOIN FETCH` 기반 조회로 변경하고, Hibernate Statistics 테스트로 SQL 실행 횟수를 검증했습니다. 또한 감사 로그 최신 100건 조회에 맞춰 `created_at DESC` 인덱스를 추가해 최신순 조회 경로를 최적화했습니다.**

---

## 12. 공식문서 참고

| 주제                                  | 공식문서                                |
| ----------------------------------- | ----------------------------------- |
| Hibernate fetching / join fetching  | Hibernate ORM User Guide            |
| Hibernate Statistics                | Hibernate Statistics Javadoc        |
| PostgreSQL Index + ORDER BY + LIMIT | PostgreSQL Indexes and ORDER BY     |
| Spring Boot `ddl-auto`              | Spring Boot Database Initialization |
