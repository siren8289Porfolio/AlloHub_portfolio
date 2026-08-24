package com.allochub.ai;

import com.allochub.audit.AuditLog;
import com.allochub.audit.AuditLogRepository;
import com.allochub.domain.distribution.Distribution;
import com.allochub.domain.distribution.DistributionRepository;
import com.allochub.domain.investment.Investment;
import com.allochub.domain.investment.InvestmentRepository;
import com.allochub.domain.investment.InvestorInvestment;
import com.allochub.global.exception.AppException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiEvidenceService {

    private static final String STATUS = "DESIGNED / PROPOSED / PARTIALLY IMPLEMENTED / NOT TESTED IN PRODUCTION";

    private final InvestmentRepository investmentRepository;
    private final DistributionRepository distributionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiClient aiClient;

    public AiEvidenceService(
            InvestmentRepository investmentRepository,
            DistributionRepository distributionRepository,
            AuditLogRepository auditLogRepository,
            AiClient aiClient) {
        this.investmentRepository = investmentRepository;
        this.distributionRepository = distributionRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiClient = aiClient;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> explainInvestment(AiEvidenceRequest request) {
        String investmentId = normalize(request.investmentId());
        String companyName = normalize(request.companyName());
        if (investmentId == null && companyName == null) {
            throw AppException.invalidInput("investmentId 또는 companyName을 입력하세요");
        }

        List<Investment> investments = findInvestments(investmentId, companyName);
        if (investments.isEmpty()) {
            throw AppException.invalidInput("조회 가능한 투자 원장 근거가 없습니다");
        }

        List<String> investmentIds = investments.stream().map(Investment::getId).toList();
        List<Distribution> distributions =
                distributionRepository.findByInvestmentIdInOrderByDistributionDateDesc(investmentIds);
        List<String> distributionIds = distributions.stream().map(Distribution::getId).toList();
        List<AuditLog> investmentAuditLogs =
                auditLogRepository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(
                        "Investment", investmentIds);
        List<AuditLog> distributionAuditLogs = distributionIds.isEmpty()
                ? List.of()
                : auditLogRepository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(
                        "Distribution", distributionIds);

        Map<String, Object> internalFacts = buildInternalFacts(investments, distributions);
        String resolvedCompanyName = investments.stream()
                .map(Investment::getCompanyName)
                .findFirst()
                .orElse(companyName);

        Map<String, Object> evidenceResponse = aiClient.queryEvidence(resolvedCompanyName, investmentId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evidenceItems = evidenceResponse.get("evidence") instanceof List<?> list
                ? list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList()
                : List.of();

        Map<String, Object> explanationResponse =
                aiClient.explain(resolvedCompanyName, investmentId, internalFacts, evidenceItems);

        String summary = explanationResponse.get("explanation") instanceof String explanation
                        && Boolean.TRUE.equals(explanationResponse.get("available"))
                ? explanation
                : buildSummary(investments, distributions, investmentAuditLogs, distributionAuditLogs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", STATUS);
        response.put("query", buildQuery(investmentId, companyName));
        response.put("summary", summary);
        response.put("internalLedgerFacts", internalFacts);
        response.put("externalEvidence", mergeExternalEvidence(evidenceResponse, evidenceItems));
        response.put("aiService", buildAiServiceStatus(evidenceResponse, explanationResponse));
        response.put(
                "differencesAndCautions",
                buildCautions(evidenceResponse, explanationResponse));
        response.put(
                "sources",
                buildSources(investments, distributions, investmentAuditLogs, distributionAuditLogs, evidenceResponse));
        response.put("humanDecisionBoundary", buildDecisionBoundary());
        return response;
    }

    private List<Investment> findInvestments(String investmentId, String companyName) {
        if (investmentId != null) {
            Investment investment = investmentRepository
                    .findById(investmentId)
                    .orElseThrow(() -> AppException.notFound("조회 가능한 투자 원장 근거가 없습니다"));
            return List.of(investment);
        }
        return investmentRepository.findByCompanyNameContainingIgnoreCaseOrderByInvestmentDateDesc(companyName);
    }

    private Map<String, Object> buildQuery(String investmentId, String companyName) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("investmentId", investmentId);
        query.put("companyName", companyName);
        return query;
    }

    private String buildSummary(
            List<Investment> investments,
            List<Distribution> distributions,
            List<AuditLog> investmentAuditLogs,
            List<AuditLog> distributionAuditLogs) {
        int totalInvestment = investments.stream().mapToInt(Investment::getInvestmentAmount).sum();
        int totalDistribution = distributions.stream().mapToInt(Distribution::getDistributionAmount).sum();
        int auditCount = investmentAuditLogs.size() + distributionAuditLogs.size();
        String companyNames = investments.stream()
                .map(Investment::getCompanyName)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("확인된 기업 없음");

        return "%s 관련 내부 원장 기준 투자 %d건, 투자금 합계 %d, 배분 %d건, 배분금 합계 %d, 관련 감사 로그 %d건을 확인했습니다. FastAPI 설명이 불가하여 Spring deterministic summary로 대체했습니다."
                .formatted(
                        companyNames,
                        investments.size(),
                        totalInvestment,
                        distributions.size(),
                        totalDistribution,
                        auditCount);
    }

    private Map<String, Object> buildInternalFacts(
            List<Investment> investments, List<Distribution> distributions) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("investments", investments.stream().map(this::investmentFact).toList());
        facts.put("distributions", distributions.stream().map(this::distributionFact).toList());

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("investmentCount", investments.size());
        totals.put("investmentAmount", investments.stream().mapToInt(Investment::getInvestmentAmount).sum());
        totals.put("distributionCount", distributions.size());
        totals.put("distributionAmount", distributions.stream().mapToInt(Distribution::getDistributionAmount).sum());
        facts.put("totals", totals);
        return facts;
    }

    private Map<String, Object> investmentFact(Investment investment) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("source", "Investment");
        fact.put("investmentId", investment.getId());
        fact.put("companyName", investment.getCompanyName());
        fact.put("investmentAmount", investment.getInvestmentAmount());
        fact.put("investmentDate", investment.getInvestmentDate().toString());
        fact.put("status", investment.getStatus().name());

        List<Map<String, Object>> allocations = new ArrayList<>();
        for (InvestorInvestment allocation : investment.getInvestorInvestments()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source", "InvestorInvestment");
            item.put("investorId", allocation.getInvestor().getId());
            item.put("investorName", allocation.getInvestor().getName());
            item.put("allocationRatio", allocation.getInvestor().getAllocationRatio());
            item.put("allocatedAmount", allocation.getAllocatedAmount());
            allocations.add(item);
        }
        fact.put("investorAllocations", allocations);
        return fact;
    }

    private Map<String, Object> distributionFact(Distribution distribution) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("source", "Distribution");
        fact.put("distributionId", distribution.getId());
        fact.put("investmentId", distribution.getInvestment().getId());
        fact.put("companyName", distribution.getInvestment().getCompanyName());
        fact.put("distributionAmount", distribution.getDistributionAmount());
        fact.put("distributionType", distribution.getDistributionType());
        fact.put("distributionDate", distribution.getDistributionDate().toString());
        fact.put("status", distribution.getStatus().name());
        return fact;
    }

    private List<Map<String, Object>> mergeExternalEvidence(
            Map<String, Object> evidenceResponse, List<Map<String, Object>> evidenceItems) {
        if (!evidenceItems.isEmpty()) {
            return evidenceItems;
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("source", "FastAPI AI Service");
        fallback.put("scope", "OpenDART / 공공데이터 Evidence");
        fallback.put("status", String.valueOf(evidenceResponse.getOrDefault("status", "UNAVAILABLE")));
        fallback.put(
                "message",
                String.valueOf(evidenceResponse.getOrDefault("reason", "external evidence unavailable")));
        return List.of(fallback);
    }

    private Map<String, Object> buildAiServiceStatus(
            Map<String, Object> evidenceResponse, Map<String, Object> explanationResponse) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("evidenceAvailable", Boolean.TRUE.equals(evidenceResponse.get("available")));
        status.put("explanationAvailable", Boolean.TRUE.equals(explanationResponse.get("available")));
        status.put("evidenceStatus", evidenceResponse.getOrDefault("status", "UNAVAILABLE"));
        status.put("explanationStatus", explanationResponse.getOrDefault("status", "UNAVAILABLE"));
        status.put(
                "boundary",
                "FastAPI AI Service failures are isolated; ledger transactions are unaffected.");
        return status;
    }

    private List<String> buildCautions(
            Map<String, Object> evidenceResponse, Map<String, Object> explanationResponse) {
        List<String> cautions = new ArrayList<>();
        cautions.add("AI 응답은 Investment, Distribution, AuditLog를 자동 변경하지 않습니다.");
        cautions.add("Spring Backend가 원장 정합성의 최종 소유자이며 FastAPI는 Evidence/설명만 담당합니다.");
        if (!Boolean.TRUE.equals(evidenceResponse.get("available"))) {
            cautions.add("외부 Evidence 조회가 불가하여 evidence unavailable 상태로 표시합니다.");
        }
        if (!Boolean.TRUE.equals(explanationResponse.get("available"))) {
            cautions.add("FastAPI 설명이 불가하여 내부 원장 기반 deterministic summary로 대체합니다.");
        }
        cautions.add("중요한 금융/운영 판단은 사용자 확인 대상입니다.");
        return cautions;
    }

    private List<Map<String, Object>> buildSources(
            List<Investment> investments,
            List<Distribution> distributions,
            List<AuditLog> investmentAuditLogs,
            List<AuditLog> distributionAuditLogs,
            Map<String, Object> evidenceResponse) {
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(source("Investor", "출자자 배분 context", "internal-ledger", investments.size()));
        sources.add(source("Investment", "투자 원장", "internal-ledger", investments.size()));
        sources.add(source("InvestorInvestment", "투자별 출자자 배분", "internal-ledger", investments.size()));
        sources.add(source("Distribution", "배분 원장", "internal-ledger", distributions.size()));
        sources.add(source("DistributionDetail", "출자자별 배분 상세", "internal-ledger", distributions.size()));
        sources.add(source(
                "AuditLog",
                "투자/배분 변경 이력",
                "internal-ledger",
                investmentAuditLogs.size() + distributionAuditLogs.size()));
        sources.add(source(
                "FastAPI AI Service",
                "Evidence/LLM 설명",
                Boolean.TRUE.equals(evidenceResponse.get("available"))
                        ? "external-ai-service"
                        : "external-ai-service-unavailable",
                0));
        return sources;
    }

    private Map<String, Object> source(String name, String description, String type, int rowCount) {
        Map<String, Object> source = new HashMap<>();
        source.put("name", name);
        source.put("description", description);
        source.put("type", type);
        source.put("rowCount", rowCount);
        source.put("retrievedAt", Instant.now().toString());
        return source;
    }

    private List<String> buildDecisionBoundary() {
        return List.of(
                "투자 여부 추천 또는 자동 의사결정을 하지 않습니다.",
                "예상 수익률을 생성하지 않습니다.",
                "원장 금액 계산, 수정, 배분 승인 권한을 갖지 않습니다.",
                "외부 정보만으로 내부 원장 값을 덮어쓰지 않습니다.");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
