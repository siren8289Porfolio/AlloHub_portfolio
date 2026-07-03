package com.allochub.controller;

import com.allochub.domain.investment.InvestmentRequest;
import com.allochub.domain.investment.InvestmentService;
import com.allochub.global.response.ApiResponse;
import com.allochub.global.security.AuthInterceptor;
import com.allochub.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<Map<String, Object>>>> list() {
        return ApiResponse.ok(Map.of("investments", investmentService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            HttpServletRequest request, @RequestBody InvestmentRequest body) {
        AuthUser user = AuthInterceptor.requireUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(investmentService.create(user, body)));
    }
}
