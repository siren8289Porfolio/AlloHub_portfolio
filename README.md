# DA Portfolio README Guide (Reddit 기준)

이 브랜치 README는 **데이터 분석 포트폴리오용 구조**만 다룬다.

## 1. 레딧 추천 핵심 4대 프로젝트 유형

### 유형 1) End-to-End 비즈니스 인사이트
- 문제 정의 -> SQL/Python 분석 -> 대시보드 -> 액션 플랜 제안
- 추천 데이터: 이커머스 이탈, 라이드헤일링 이용 로그, 구독 서비스 이벤트

### 유형 2) 파이프라인 자동화 & 스크래핑
- API/스크래핑 수집 -> DB 적재/변환 -> 자동 업데이트 대시보드
- 추천 데이터: 부동산 매물, 채용 공고, 주가/암호화폐 추세

### 유형 3) A/B 테스팅 & 통계 분석
- 가설 설정 -> 실험 설계 -> 유의성 검정(p-value, t-test) -> 의사결정
- 추천 데이터: 게임 리텐션, 웹 CTR 실험

### 유형 4) SQL 딥다이브
- CTE, Window Function, Complex Join으로 복합 지표 도출
- 추천 데이터: Olist E-commerce, 공유 자전거 이용 데이터

## 2. 표준 GitHub README 템플릿 (복붙용)

아래 블록을 각 프로젝트 README에 그대로 복사해서 채우면 된다.

```md
# [Project Title]
한 줄 요약: [어떤 비즈니스 문제를 어떤 데이터로 해결했는지]

![dashboard-cover](./assets/dashboard-cover.png)
Live Dashboard: [Tableau Public / Power BI / Streamlit URL]

## 1) Business Problem
- 해결하려는 문제:
- 분석 목적:
- 성공 기준(KPI):

## 2) Data & Tech Stack
- Data Source:
- 기간/행 수/주요 컬럼:
- Tools: Python, SQL, Tableau/Power BI, GitHub

## 3) Data Cleaning & Analysis
- 결측/이상치 처리:
- 핵심 전처리 로직:
- 분석 방법(세그먼트, 코호트, 퍼널, 실험 분석 등):

## 4) Key Insights & Dashboard
1. [인사이트 1: 수치 중심]
2. [인사이트 2: 수치 중심]
3. [인사이트 3: 수치 중심]
4. [선택 인사이트 4]

Dashboard Preview:
- [차트 1 설명]
- [차트 2 설명]

## 5) Business Recommendations
1. [실행안 1: 기대 효과와 우선순위]
2. [실행안 2: 비용 대비 효과]
3. [Next Step: 실험/배포/모니터링 계획]
```

## 3. 감점/가점 체크리스트

### 감점 포인트
- Titanic, Iris, Boston Housing 같은 너무 흔한 데이터셋 단독 사용
- 기술 설명만 있고 비즈니스 의사결정 문장이 없는 경우
- 결과가 정적 이미지만 있고 클릭 가능한 라이브 링크가 없는 경우

### 가점 포인트
- 공공데이터/API/스크래핑으로 직접 수집한 데이터 포함
- "상관계수 0.75"보다 "고객 유지율 12% 개선"처럼 성과 중심 문장 사용
- README 상단에 라이브 링크와 대시보드 대표 캡처 배치

## 4. 작성 원칙 (현직자 관점)

- 3초 룰: 상단에서 문제/성과/대시보드를 바로 보여준다.
- 숫자 우선: 인사이트는 반드시 수치와 기간을 같이 쓴다.
- 실행 가능성: 추천안은 "누가, 언제, 무엇을"까지 포함한다.
- 재현 가능성: 데이터 출처, 전처리 단계, 쿼리/노트북 위치를 명시한다.
