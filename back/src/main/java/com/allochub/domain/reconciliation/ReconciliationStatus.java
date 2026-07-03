package com.allochub.domain.reconciliation;

import java.util.List;

public record ReconciliationStatus(
        int totalFund,
        int totalInvestment,
        int cashBalance,
        double totalRatio,
        int totalDistribution,
        boolean isValid,
        List<String> messages) {}
