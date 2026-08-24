from app.config import settings
from app.schemas import ExplainRequest, ExplainResponse


def explain(request: ExplainRequest) -> ExplainResponse:
    """Build a grounded deterministic explanation from provided context only.

    Never mutates AllocHub ledger entities. Does not invent unsupported claims.
    """
    company = (request.company_name or "").strip() or "대상 기업"
    context = request.internal_context or {}
    totals = context.get("totals") if isinstance(context.get("totals"), dict) else {}
    investment_count = totals.get("investmentCount", 0)
    investment_amount = totals.get("investmentAmount", 0)
    distribution_count = totals.get("distributionCount", 0)
    distribution_amount = totals.get("distributionAmount", 0)

    evidence_statuses = [
        str(item.get("status", "UNKNOWN"))
        for item in (request.evidence or [])
        if isinstance(item, dict)
    ]
    external_available = any(status in {"AVAILABLE", "PARTIAL", "CONFIGURED_STUB"} for status in evidence_statuses)

    explanation = (
        f"{company}에 대한 설명은 Spring이 전달한 내부 원장 context만 사용합니다. "
        f"투자 {investment_count}건(합계 {investment_amount}), "
        f"배분 {distribution_count}건(합계 {distribution_amount})입니다. "
        "이 응답은 원장 금액을 변경하거나 투자 판단을 대신하지 않습니다."
    )
    if not external_available:
        explanation += " 외부 Evidence는 현재 확정 사실로 사용할 수 없습니다."

    caveats = [
        "AI Service는 Investment/Distribution/AuditLog를 수정하지 않습니다.",
        "내부 원장 수치가 authoritative source이며 외부 Evidence는 enrichment입니다.",
        "근거가 없는 내용은 확정 사실로 생성하지 않습니다.",
        "중요한 금융/운영 판단은 사용자 확인이 필요합니다.",
    ]
    if settings.llm_enabled:
        caveats.append("LLM provider 연동은 설계만 존재하며 production evaluation은 완료되지 않았습니다.")
    else:
        caveats.append("생성형 LLM은 비활성 상태이며 deterministic grounded summary만 제공합니다.")

    return ExplainResponse(
        status="OK",
        available=True,
        explanation=explanation,
        grounded=True,
        caveats=caveats,
    )
