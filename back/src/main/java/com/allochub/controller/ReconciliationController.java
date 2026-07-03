package com.allochub.controller;

import com.allochub.domain.reconciliation.ReconciliationService;
import com.allochub.domain.reconciliation.ReconciliationStatus;
import com.allochub.global.response.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getStatus() {
        ReconciliationStatus status = reconciliationService.getStatus();
        Map<String, Object> data = new HashMap<>();
        data.put("totalFund", status.totalFund());
        data.put("totalInvestment", status.totalInvestment());
        data.put("cashBalance", status.cashBalance());
        data.put("totalRatio", status.totalRatio());
        data.put("totalDistribution", status.totalDistribution());
        data.put("isValid", status.isValid());
        data.put("messages", status.messages());
        return ApiResponse.ok(data);
    }
}
