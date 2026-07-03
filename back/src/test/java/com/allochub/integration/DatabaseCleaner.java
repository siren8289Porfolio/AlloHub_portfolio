package com.allochub.integration;

import com.allochub.audit.AuditLogRepository;
import com.allochub.domain.distribution.DistributionRepository;
import com.allochub.domain.investment.InvestmentRepository;
import com.allochub.domain.investor.InvestorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    @Autowired
    private DistributionRepository distributionRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void clean() {
        distributionRepository.deleteAll();
        investmentRepository.deleteAll();
        investorRepository.deleteAll();
        auditLogRepository.deleteAll();
    }
}
