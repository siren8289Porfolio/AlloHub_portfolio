package com.allochub.domain.reconciliation;

import com.allochub.domain.distribution.Distribution;
import com.allochub.domain.distribution.DistributionRepository;
import com.allochub.domain.investment.InvestmentRepository;
import com.allochub.domain.investor.InvestorRepository;
import com.allochub.global.exception.AppException;
import com.allochub.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private final InvestorRepository investorRepository;
    private final InvestmentRepository investmentRepository;
    private final DistributionRepository distributionRepository;

    public ReconciliationService(
            InvestorRepository investorRepository,
            InvestmentRepository investmentRepository,
            DistributionRepository distributionRepository) {
        this.investorRepository = investorRepository;
        this.investmentRepository = investmentRepository;
        this.distributionRepository = distributionRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationStatus getStatus() {
        int totalFund = investorRepository.sumInvestmentAmount();
        double totalRatio = investorRepository.sumAllocationRatio();
        int totalInvestment = investmentRepository.sumInvestmentAmount();
        int cashBalance = totalFund - totalInvestment;

        List<Distribution> distributions = distributionRepository.findAll();
        int totalDistribution =
                distributions.stream().mapToInt(Distribution::getDistributionAmount).sum();

        List<String> messages = new ArrayList<>();
        boolean ratioValid = totalRatio <= 100.0001;
        boolean investmentValid = totalInvestment <= totalFund;
        boolean capitalBalanced = totalInvestment + cashBalance == totalFund;

        if (!ratioValid) messages.add("배분 비율이 100%를 초과합니다");
        if (!investmentValid) messages.add("투자금액이 총 출자금을 초과합니다");
        if (!capitalBalanced) messages.add("정합성 검증 실패");

        boolean distributionValid = true;
        for (Distribution distribution : distributions) {
            int detailSum = distribution.getDetails().stream()
                    .mapToInt(d -> d.getDistributedAmount())
                    .sum();
            if (detailSum != distribution.getDistributionAmount()) {
                distributionValid = false;
                messages.add("정합성 검증 실패");
                break;
            }
        }

        boolean isValid = ratioValid && investmentValid && capitalBalanced && distributionValid;
        if (isValid && messages.isEmpty()) {
            messages.add("모든 정합성 검증을 통과했습니다");
        }

        return new ReconciliationStatus(
                totalFund,
                totalInvestment,
                cashBalance,
                totalRatio,
                totalDistribution,
                isValid,
                messages);
    }

    public void assertValid(ReconciliationStatus status) {
        if (!status.isValid()) {
            throw new AppException(
                    ErrorCode.RECONCILIATION_FAILED,
                    "정합성 검증 실패: 투자금 + 현금 ≠ 총 출자금",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
