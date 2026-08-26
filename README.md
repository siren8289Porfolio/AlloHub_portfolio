# DA Portfolio README (Reddit 표준 형식)

## README 섹션 구성 표준

| 섹션 | 수록 내용 | Reddit 현직자 작성 팁 |
| --- | --- | --- |
| Project Title & Summary | 프로젝트 제목, 한 줄 요약, 핵심 대시보드 캡처 | 대시보드 완성본 이미지를 상단에 배치해 3초 내 흥미 유발 |
| Business Problem | 해결하려는 문제, 분석 목적 | "기술 연마용" 대신 "매출 증대/이탈 방지" 같은 비즈니스 언어 사용 |
| Data & Tech Stack | 데이터 출처, 스크립트, 사용 기술(Python, SQL, Tableau 등) | 기술 스택을 직관적인 리스트 형태로 정리 |
| Data Cleaning & Analysis | 전처리 로직, 주요 데이터 변환 과정 | 전체 코드 대신 핵심 전처리/분석 로직만 요약 |
| Key Insights & Dashboard | 차트 기반 핵심 인사이트 3~4개 | 숫자+그래프 중심으로 쓰고 라이브 대시보드 링크 포함 |
| Business Recommendations | 실행 가능한 제안(Next Steps) | 면접관이 가장 많이 보는 파트, 가성비 높은 실행안 제시 |

## 표준 GitHub README 템플릿

```md
# [Project Title]
한 줄 요약: [비즈니스 문제 + 데이터 + 결과]

![dashboard-cover](./assets/dashboard-cover.png)
Live Dashboard: [Tableau Public / Power BI / Streamlit URL]

## Business Problem
- 해결하려는 문제:
- 분석 목적:
- KPI:

## Data & Tech Stack
- Data Source:
- Data Range / Rows:
- Tools: Python, SQL, Tableau(Power BI), GitHub

## Data Cleaning & Analysis
- 결측치/이상치 처리:
- 주요 파생변수 생성:
- 핵심 분석 로직(세그먼트, 코호트, 퍼널, A/B 등):

## Key Insights & Dashboard
1. [인사이트 1: 수치 중심]
2. [인사이트 2: 수치 중심]
3. [인사이트 3: 수치 중심]
4. [선택 인사이트 4]

## Business Recommendations
1. [실행안 1: 기대효과/우선순위]
2. [실행안 2: 비용 대비 효과]
3. [Next Steps: 실험/배포/모니터링]
```

## 감점/가점 체크리스트

- 감점: Titanic/Iris/Boston Housing 단독 사용, 비즈니스 문장 부재, 라이브 링크 없음
- 가점: 직접 수집 데이터 포함, 성과 중심 문장(예: 유지율 12% 개선), 상단 대시보드 캡처+라이브 URL 배치
