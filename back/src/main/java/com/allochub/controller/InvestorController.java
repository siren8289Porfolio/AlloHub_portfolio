package com.allochub.controller;

import com.allochub.domain.investor.InvestorRequest;
import com.allochub.domain.investor.InvestorService;
import com.allochub.global.response.ApiResponse;
import com.allochub.global.security.AuthInterceptor;
import com.allochub.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investors")
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.ok(investorService.list());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            HttpServletRequest request, @RequestBody InvestorRequest body) {
        AuthUser user = AuthInterceptor.requireUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(investorService.create(user, body), "등록 완료"));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
            HttpServletRequest request, @PathVariable String id, @RequestBody InvestorRequest body) {
        AuthUser user = AuthInterceptor.requireUser(request);
        return ApiResponse.ok(investorService.update(user, id, body), "수정 완료");
    }
}
