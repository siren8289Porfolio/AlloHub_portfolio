package com.allochub.domain.investment;

import com.allochub.audit.AuditService;
import com.allochub.domain.investor.Investor;
import com.allochub.domain.investor.InvestorRepository;
import com.allochub.domain.reconciliation.ReconciliationService;
import com.allochub.global.exception.AppException;
import com.allochub.global.security.AuthUser;
import com.allochub.lib.AllocationCalculator;
import com.allochub.lib.AllocationCalculator.AllocationResult;
import com.allochub.lib.AllocationCalculator.RatioItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestorRepository investorRepository;
    private final ReconciliationService reconciliationService;
    private final AuditService auditService;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            InvestorRepository investorRepository,
            ReconciliationService reconciliationService,
            AuditService auditService) {
        this.investmentRepository = investmentRepository;
        this.investorRepository = investorRepository;
        this.reconciliationService = reconciliationService;
        this.auditService = auditService;
    }

    @Transactional
    public Map<String, Object> create(AuthUser user, InvestmentRequest request) {
        if (request.companyName().isBlank() || request.investmentAmount() <= 0) {
            throw AppException.invalidInput("필수 항목을 입력하세요");
        }

        List<Investor> investors = investorRepository.findAllByOrderByCreatedAtAsc();
        if (investors.isEmpty()) {
            throw AppException.invalidInput("필수 항목을 입력하세요");
        }

        int totalFund = investorRepository.sumInvestmentAmount();
        int totalInvestment = investmentRepository.sumInvestmentAmount();
        if (totalInvestment + request.investmentAmount() > totalFund) {
            throw AppException.invalidInvestmentAmount();
        }

        List<RatioItem> ratios =
                investors.stream().map(i -> new RatioItem(i.getId(), i.getAllocationRatio())).toList();
        List<AllocationResult> allocations =
                AllocationCalculator.allocateByRatio(request.investmentAmount(), ratios);

        Investment investment = new Investment();
        investment.setCompanyName(request.companyName());
        investment.setInvestmentAmount(request.investmentAmount());
        investment.setCreatedBy(user.id());

        for (AllocationResult allocation : allocations) {
            Investor investor = investors.stream()
                    .filter(i -> i.getId().equals(allocation.id()))
                    .findFirst()
                    .orElseThrow();
            InvestorInvestment ii = new InvestorInvestment();
            ii.setInvestor(investor);
            ii.setInvestment(investment);
            ii.setAllocatedAmount(allocation.amount());
            investment.getInvestorInvestments().add(ii);
        }

        investment = investmentRepository.save(investment);

        var reconciliation = reconciliationService.getStatus();
        reconciliationService.assertValid(reconciliation);
        auditService.logCreate(user, "Investment", investment.getId(), investment);

        return toResponse(investment, reconciliation);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return investmentRepository.findAll().stream().map(this::toListItem).toList();
    }

    private Map<String, Object> toResponse(
            Investment investment, com.allochub.domain.reconciliation.ReconciliationStatus reconciliation) {
        Map<String, Object> map = new HashMap<>();
        map.put("investmentId", investment.getId());
        map.put("companyName", investment.getCompanyName());
        map.put("investmentAmount", investment.getInvestmentAmount());

        List<Map<String, Object>> allocations = new ArrayList<>();
        for (InvestorInvestment ii : investment.getInvestorInvestments()) {
            Map<String, Object> a = new HashMap<>();
            a.put("investorId", ii.getInvestor().getId());
            a.put("investorName", ii.getInvestor().getName());
            a.put("allocatedAmount", ii.getAllocatedAmount());
            allocations.add(a);
        }
        map.put("investorAllocations", allocations);

        Map<String, Object> recon = new HashMap<>();
        recon.put("totalFund", reconciliation.totalFund());
        recon.put("totalInvestment", reconciliation.totalInvestment());
        recon.put("cashBalance", reconciliation.cashBalance());
        recon.put("isValid", reconciliation.isValid());
        map.put("reconciliation", recon);
        return map;
    }

    private Map<String, Object> toListItem(Investment investment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", investment.getId());
        map.put("companyName", investment.getCompanyName());
        map.put("investmentAmount", investment.getInvestmentAmount());
        map.put("investmentDate", investment.getInvestmentDate().toString());
        map.put("status", investment.getStatus().name());

        List<Map<String, Object>> iis = new ArrayList<>();
        for (InvestorInvestment ii : investment.getInvestorInvestments()) {
            Map<String, Object> item = new HashMap<>();
            item.put("investorId", ii.getInvestor().getId());
            item.put("investorName", ii.getInvestor().getName());
            item.put("allocationRatio", ii.getInvestor().getAllocationRatio());
            item.put("allocatedAmount", ii.getAllocatedAmount());
            iis.add(item);
        }
        map.put("investorInvestments", iis);
        return map;
    }
}
