package com.allochub.controller;

import com.allochub.domain.distribution.DistributionRequest;
import com.allochub.domain.distribution.DistributionService;
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
@RequestMapping("/api/distributions")
public class DistributionController {

    private final DistributionService distributionService;

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<Map<String, Object>>>> list() {
        return ApiResponse.ok(Map.of("distributions", distributionService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            HttpServletRequest request, @RequestBody DistributionRequest body) {
        AuthUser user = AuthInterceptor.requireUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(distributionService.create(user, body)));
    }

    @PostMapping("/calculate")
    public ApiResponse<Map<String, Object>> calculate(@RequestBody DistributionRequest body) {
        return ApiResponse.ok(distributionService.calculate(body));
    }
}
