from datetime import datetime, timezone

from app.config import settings
from app.schemas import EvidenceItem, EvidenceQueryRequest, EvidenceQueryResponse


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def query_evidence(request: EvidenceQueryRequest) -> EvidenceQueryResponse:
    """Return external Evidence metadata.

    This service never writes AllocHub ledger data. Without OpenDART credentials
    it returns an explicit stub so callers can degrade gracefully.
    """
    company = (request.company_name or "").strip() or None
    investment_id = (request.investment_id or "").strip() or None
    query = {
        "companyName": company,
        "investmentId": investment_id,
        "corpCode": request.corp_code,
    }

    if not company and not investment_id and not request.corp_code:
        return EvidenceQueryResponse(
            status="INVALID_QUERY",
            query=query,
            evidence=[],
            available=False,
        )

    fetched_at = _now_iso()
    if settings.opendart_api_key:
        evidence = [
            EvidenceItem(
                source="OpenDART",
                scope="기업개황/공시검색 (configured key; live fetch not production-verified)",
                status="CONFIGURED_STUB",
                message=(
                    "OpenDART API key is configured, but live retrieval is not production-verified. "
                    "Treat results as non-authoritative Evidence only."
                ),
                source_key=request.corp_code or company,
                reference_date=None,
                fetched_at=fetched_at,
                facts=[],
            )
        ]
        status = "PARTIAL"
        available = True
    else:
        evidence = [
            EvidenceItem(
                source="OpenDART",
                scope="기업/공시 원문/메타데이터",
                status="UNAVAILABLE",
                message="OpenDART API key is not configured. External evidence is unavailable.",
                source_key=None,
                reference_date=None,
                fetched_at=fetched_at,
                facts=[],
            ),
            EvidenceItem(
                source="KVIC",
                scope="벤처투자 관련 공식 통계/공시",
                status="NOT_IMPLEMENTED",
                message="KVIC reference retrieval is designed but not implemented in this service.",
                source_key=None,
                reference_date=None,
                fetched_at=fetched_at,
                facts=[],
            ),
            EvidenceItem(
                source="KRX",
                scope="상장회사/시장 reference",
                status="NOT_IMPLEMENTED",
                message="KRX company master retrieval is designed but not implemented in this service.",
                source_key=None,
                reference_date=None,
                fetched_at=fetched_at,
                facts=[],
            ),
        ]
        status = "UNAVAILABLE"
        available = False

    return EvidenceQueryResponse(
        status=status,
        query=query,
        evidence=evidence,
        available=available,
    )
