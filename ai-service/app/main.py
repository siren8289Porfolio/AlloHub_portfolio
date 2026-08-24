from fastapi import FastAPI

from app.config import settings
from app.evidence import query_evidence
from app.explain import explain
from app.schemas import (
    EvidenceQueryRequest,
    EvidenceQueryResponse,
    ExplainRequest,
    ExplainResponse,
)

app = FastAPI(
    title="AlloHub AI Service",
    description=(
        "Evidence lookup and grounded explanation assistant. "
        "Does not own or mutate AllocHub ledger transactions."
    ),
    version="0.1.0",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": settings.service_name}


@app.post("/ai/evidence/query", response_model=EvidenceQueryResponse)
def evidence_query(body: EvidenceQueryRequest) -> EvidenceQueryResponse:
    return query_evidence(body)


@app.post("/ai/explain", response_model=ExplainResponse)
def explain_endpoint(body: ExplainRequest) -> ExplainResponse:
    return explain(body)
