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

    private static final String STATUS = "DESIGNED / PROPOSED / NOT IMPLEMENTED / NOT TESTED";

    private final InvestmentRepository investmentRepository;
    private final DistributionRepository distributionRepository;
    private final AuditLogRepository auditLogRepository;

    public AiEvidenceService(
            InvestmentRepository investmentRepository,
            DistributionRepository distributionRepository,
            AuditLogRepository auditLogRepository) {
        this.investmentRepository = investmentRepository;
        this.distributionRepository = distributionRepository;
        this.auditLogRepository = auditLogRepository;
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", STATUS);
        response.put("query", buildQuery(investmentId, companyName));
        response.put("summary", buildSummary(investments, distributions, investmentAuditLogs, distributionAuditLogs));
        response.put("internalLedgerFacts", buildInternalFacts(investments, distributions));
        response.put("externalEvidence", buildExternalEvidence());
        response.put("differencesAndCautions", buildCautions());
        response.put("sources", buildSources(investments, distributions, investmentAuditLogs, distributionAuditLogs));
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

        return "%s 관련 내부 원장 기준 투자 %d건, 투자금 합계 %d, 배분 %d건, 배분금 합계 %d, 관련 감사 로그 %d건을 확인했습니다. 외부 Evidence와 생성형 요약은 아직 구현/검증되지 않았습니다."
                .formatted(companyNames, investments.size(), totalInvestment, distributions.size(), totalDistribution, auditCount);
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

    private List<Map<String, Object>> buildExternalEvidence() {
        return List.of(
                externalEvidence("OpenDART", "기업/공시 원문/메타데이터"),
                externalEvidence("KVIC", "벤처투자 관련 공식 통계/공시"),
                externalEvidence("KRX", "상장회사/시장 reference"));
    }

    private Map<String, Object> externalEvidence(String source, String scope) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", source);
        evidence.put("scope", scope);
        evidence.put("status", "NOT_IMPLEMENTED");
        evidence.put("message", "현재 외부 evidence retrieval 구현 및 검증 증거가 없어 확정 사실로 사용하지 않습니다.");
        return evidence;
    }

    private List<String> buildCautions() {
        return List.of(
                "AI 응답은 Investment, Distribution, AuditLog를 자동 변경하지 않습니다.",
                "외부 source retrieval은 아직 구현되지 않아 내부 원장과 외부 공시 값의 차이를 계산하지 않습니다.",
                "생성형 AI provider와 evaluation dataset이 정해지기 전까지 unsupported claim을 만들지 않는 deterministic 요약만 제공합니다.",
                "중요한 금융/운영 판단은 사용자 확인 대상입니다.");
    }

    private List<Map<String, Object>> buildSources(
            List<Investment> investments,
            List<Distribution> distributions,
            List<AuditLog> investmentAuditLogs,
            List<AuditLog> distributionAuditLogs) {
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
        sources.add(source("OpenDART", "기업/공시 원문/메타데이터", "external-evidence-not-implemented", 0));
        sources.add(source("KVIC", "벤처투자 공식 통계/공시", "external-evidence-not-implemented", 0));
        sources.add(source("KRX", "상장회사/시장 reference", "external-evidence-not-implemented", 0));
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
