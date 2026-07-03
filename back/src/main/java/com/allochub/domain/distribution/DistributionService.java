package com.allochub.domain.distribution;

import com.allochub.audit.AuditService;
import com.allochub.domain.investment.Investment;
import com.allochub.domain.investment.InvestmentRepository;
import com.allochub.domain.investor.Investor;
import com.allochub.domain.investor.InvestorRepository;
import com.allochub.domain.investor.InvestorService;
import com.allochub.global.exception.AppException;
import com.allochub.global.security.AuthUser;
import com.allochub.lib.AllocationCalculator;
import com.allochub.lib.AllocationCalculator.AllocationResult;
import com.allochub.lib.AllocationCalculator.RatioItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributionService {

    private static final Set<String> VALID_TYPES = Set.of("배당금", "회수금");

    private final DistributionRepository distributionRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final InvestorService investorService;
    private final AuditService auditService;

    public DistributionService(
            DistributionRepository distributionRepository,
            InvestmentRepository investmentRepository,
            InvestorRepository investorRepository,
            InvestorService investorService,
            AuditService auditService) {
        this.distributionRepository = distributionRepository;
        this.investmentRepository = investmentRepository;
        this.investorRepository = investorRepository;
        this.investorService = investorService;
        this.auditService = auditService;
    }

    public Map<String, Object> calculate(DistributionRequest request) {
        validate(request);
        investmentRepository
                .findById(request.investmentId())
                .orElseThrow(() -> AppException.invalidInput("필수 항목을 입력하세요"));

        List<Map<String, Object>> details = buildDetails(request.distributionAmount());
        int totalDistributed = validateSum(details, request.distributionAmount());

        Map<String, Object> result = new HashMap<>();
        result.put("distributionAmount", request.distributionAmount());
        result.put("details", details);
        result.put("totalDistributed", totalDistributed);
        result.put("isValid", true);
        return result;
    }

    @Transactional
    public Map<String, Object> create(AuthUser user, DistributionRequest request) {
        validate(request);
        Investment investment = investmentRepository
                .findById(request.investmentId())
                .orElseThrow(() -> AppException.invalidInput("필수 항목을 입력하세요"));

        List<Map<String, Object>> details = buildDetails(request.distributionAmount());
        validateSum(details, request.distributionAmount());

        Distribution distribution = new Distribution();
        distribution.setInvestment(investment);
        distribution.setDistributionAmount(request.distributionAmount());
        distribution.setDistributionType(request.distributionType());
        distribution.setCreatedBy(user.id());

        for (Map<String, Object> detail : details) {
            DistributionDetail dd = new DistributionDetail();
            String investorId = (String) detail.get("investorId");
            Investor investor = investorRepository
                    .findById(investorId)
                    .orElseThrow(() -> AppException.invalidInput("필수 항목을 입력하세요"));
            dd.setInvestor(investor);
            dd.setDistribution(distribution);
            dd.setDistributedAmount((Integer) detail.get("distributedAmount"));
            distribution.getDetails().add(dd);
        }

        distribution = distributionRepository.save(distribution);

        for (Map<String, Object> detail : details) {
            investorService.incrementCumulativeDistribution(
                    (String) detail.get("investorId"), (Integer) detail.get("distributedAmount"));
        }

        auditService.logCreate(user, "Distribution", distribution.getId(), distribution);
        return toResponse(distribution);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return distributionRepository.findAll().stream().map(this::toListItem).toList();
    }

    private void validate(DistributionRequest request) {
        if (request.investmentId().isBlank()
                || !VALID_TYPES.contains(request.distributionType())
                || request.distributionAmount() <= 0) {
            throw AppException.invalidInput("필수 항목을 입력하세요");
        }
    }

    private List<Map<String, Object>> buildDetails(int distributionAmount) {
        List<Investor> investors = investorRepository.findAllByOrderByCreatedAtAsc();
        if (investors.isEmpty()) {
            throw AppException.invalidInput("필수 항목을 입력하세요");
        }

        List<RatioItem> ratios =
                investors.stream().map(i -> new RatioItem(i.getId(), i.getAllocationRatio())).toList();
        List<AllocationResult> allocations =
                AllocationCalculator.allocateByRatio(distributionAmount, ratios);

        List<Map<String, Object>> details = new ArrayList<>();
        for (AllocationResult allocation : allocations) {
            Investor investor = investors.stream()
                    .filter(i -> i.getId().equals(allocation.id()))
                    .findFirst()
                    .orElseThrow();
            Map<String, Object> d = new HashMap<>();
            d.put("investorId", investor.getId());
            d.put("investorName", investor.getName());
            d.put("allocationRatio", investor.getAllocationRatio());
            d.put("distributedAmount", allocation.amount());
            details.add(d);
        }
        return details;
    }

    private int validateSum(List<Map<String, Object>> details, int distributionAmount) {
        int total = details.stream().mapToInt(d -> (Integer) d.get("distributedAmount")).sum();
        if (total != distributionAmount) {
            throw AppException.distributionReconciliationFailed();
        }
        return total;
    }

    private Map<String, Object> toResponse(Distribution distribution) {
        Map<String, Object> map = new HashMap<>();
        map.put("distributionId", distribution.getId());
        map.put("investmentId", distribution.getInvestment().getId());
        map.put("companyName", distribution.getInvestment().getCompanyName());
        map.put("distributionAmount", distribution.getDistributionAmount());
        map.put("distributionType", distribution.getDistributionType());

        List<Map<String, Object>> allocations = new ArrayList<>();
        for (DistributionDetail d : distribution.getDetails()) {
            Map<String, Object> a = new HashMap<>();
            a.put("investorId", d.getInvestor().getId());
            a.put("investorName", d.getInvestor().getName());
            a.put("allocationRatio", d.getInvestor().getAllocationRatio());
            a.put("distributedAmount", d.getDistributedAmount());
            allocations.add(a);
        }
        map.put("investorAllocations", allocations);
        return map;
    }

    private Map<String, Object> toListItem(Distribution distribution) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", distribution.getId());
        map.put("investmentId", distribution.getInvestment().getId());
        map.put("companyName", distribution.getInvestment().getCompanyName());
        map.put("distributionAmount", distribution.getDistributionAmount());
        map.put("distributionType", distribution.getDistributionType());
        map.put("distributionDate", distribution.getDistributionDate().toString());
        map.put("status", distribution.getStatus().name());
        return map;
    }
}
