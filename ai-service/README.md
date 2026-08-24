# AlloHub FastAPI AI Service

Evidence 조회(`/ai/evidence/query`)와 grounded 설명(`/ai/explain`)만 담당한다.
Spring Backend 원장 DB를 직접 수정하지 않는다.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | Liveness |
| POST | `/ai/evidence/query` | OpenDART/공공데이터 Evidence 조회 (키 없으면 UNAVAILABLE) |
| POST | `/ai/explain` | 내부 context + Evidence 기반 deterministic 설명 |

## Local run

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Optional env:

- `AI_OPENDART_API_KEY` — set to mark OpenDART as configured (live fetch still stub)
- `AI_LLM_ENABLED=false` — default; keeps deterministic explanations

## Boundary

- timeout / 5xx / invalid response는 Spring `AIClient`에서 격리한다.
- AI 장애가 출자·투자·배분 transaction을 실패시키지 않는다.
