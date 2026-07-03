package com.allochub.domain.investor;

import com.allochub.audit.AuditService;
import com.allochub.global.exception.AppException;
import com.allochub.global.security.AuthUser;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestorService {

    private final InvestorRepository investorRepository;
    private final AuditService auditService;

    public InvestorService(InvestorRepository investorRepository, AuditService auditService) {
        this.investorRepository = investorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Map<String, Object> create(AuthUser user, InvestorRequest request) {
        validate(request);
        validateAllocationRatio(request.allocationRatio(), null);

        if (investorRepository.findByName(request.name()).isPresent()) {
            throw AppException.duplicate("이미 등록된 출자자입니다");
        }

        Investor investor = new Investor();
        investor.setName(request.name());
        investor.setInvestmentAmount(request.investmentAmount());
        investor.setAllocationRatio(request.allocationRatio());
        investor.setCreatedBy(user.id());

        investor = investorRepository.save(investor);
        auditService.logCreate(user, "Investor", investor.getId(), investor);
        return toResponse(investor);
    }

    @Transactional
    public Map<String, Object> update(AuthUser user, String id, InvestorRequest request) {
        validate(request);
        Investor existing =
                investorRepository.findById(id).orElseThrow(() -> AppException.invalidInput("필수 항목을 입력하세요"));
        validateAllocationRatio(request.allocationRatio(), id);

        if (!existing.getName().equals(request.name())
                && investorRepository.findByName(request.name()).isPresent()) {
            throw AppException.duplicate("이미 등록된 출자자입니다");
        }

        existing.setName(request.name());
        existing.setInvestmentAmount(request.investmentAmount());
        existing.setAllocationRatio(request.allocationRatio());
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy(user.id());

        Investor saved = investorRepository.save(existing);
        auditService.logUpdate(user, "Investor", id, existing, saved);
        return toResponse(saved);
    }

    public Map<String, Object> list() {
        List<Investor> investors = investorRepository.findAllByOrderByCreatedAtAsc();
        double totalRatio = investorRepository.sumAllocationRatio();
        int totalFund = investorRepository.sumInvestmentAmount();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFund", totalFund);
        summary.put("totalRatio", totalRatio);
        summary.put("ratioValid", totalRatio <= 100.0001);
        summary.put("count", investors.size());

        Map<String, Object> result = new HashMap<>();
        result.put("investors", investors.stream().map(this::toResponse).toList());
        result.put("summary", summary);
        return result;
    }

    @Transactional
    public void incrementCumulativeDistribution(String investorId, int amount) {
        Investor investor = investorRepository
                .findById(investorId)
                .orElseThrow(() -> AppException.invalidInput("필수 항목을 입력하세요"));
        investor.setCumulativeDistribution(investor.getCumulativeDistribution() + amount);
        investorRepository.save(investor);
    }

    public List<Investor> findAll() {
        return investorRepository.findAllByOrderByCreatedAtAsc();
    }

    private void validate(InvestorRequest request) {
        if (request.name() == null
                || request.name().isBlank()
                || request.investmentAmount() <= 0
                || request.allocationRatio() <= 0) {
            throw AppException.invalidInput("필수 항목을 입력하세요");
        }
    }

    private void validateAllocationRatio(double newRatio, String excludeId) {
        double total = investorRepository.sumAllocationRatioExcluding(excludeId) + newRatio;
        if (total > 100.0001) {
            throw AppException.invalidAllocationRatio(total);
        }
    }

    private Map<String, Object> toResponse(Investor investor) {
        Map<String, Object> map = new HashMap<>();
        map.put("investorId", investor.getId());
        map.put("id", investor.getId());
        map.put("name", investor.getName());
        map.put("investmentAmount", investor.getInvestmentAmount());
        map.put("allocationRatio", investor.getAllocationRatio());
        map.put("cumulativeDistribution", investor.getCumulativeDistribution());
        map.put("status", investor.getStatus().name());
        return map;
    }
}
