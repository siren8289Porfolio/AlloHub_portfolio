from __future__ import annotations

from typing import Any, Optional

from pydantic import BaseModel, Field


class EvidenceQueryRequest(BaseModel):
    company_name: Optional[str] = Field(default=None, alias="companyName")
    investment_id: Optional[str] = Field(default=None, alias="investmentId")
    corp_code: Optional[str] = Field(default=None, alias="corpCode")

    model_config = {"populate_by_name": True}


class EvidenceItem(BaseModel):
    source: str
    scope: str
    status: str
    message: str
    source_key: Optional[str] = Field(default=None, alias="sourceKey")
    reference_date: Optional[str] = Field(default=None, alias="referenceDate")
    fetched_at: Optional[str] = Field(default=None, alias="fetchedAt")
    facts: list[dict[str, Any]] = Field(default_factory=list)

    model_config = {"populate_by_name": True}


class EvidenceQueryResponse(BaseModel):
    status: str
    query: dict[str, Any]
    evidence: list[EvidenceItem]
    available: bool


class ExplainRequest(BaseModel):
    company_name: Optional[str] = Field(default=None, alias="companyName")
    investment_id: Optional[str] = Field(default=None, alias="investmentId")
    internal_context: dict[str, Any] = Field(default_factory=dict, alias="internalContext")
    evidence: list[dict[str, Any]] = Field(default_factory=list)

    model_config = {"populate_by_name": True}


class ExplainResponse(BaseModel):
    status: str
    available: bool
    explanation: str
    grounded: bool
    caveats: list[str]
