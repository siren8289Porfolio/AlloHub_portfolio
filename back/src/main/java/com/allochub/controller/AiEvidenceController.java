package com.allochub.controller;

import com.allochub.ai.AiEvidenceRequest;
import com.allochub.ai.AiEvidenceService;
import com.allochub.global.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiEvidenceController {

    private final AiEvidenceService aiEvidenceService;

    public AiEvidenceController(AiEvidenceService aiEvidenceService) {
        this.aiEvidenceService = aiEvidenceService;
    }

    @PostMapping("/investment-evidence")
    public ApiResponse<Map<String, Object>> explainInvestment(@RequestBody AiEvidenceRequest body) {
        return ApiResponse.ok(aiEvidenceService.explainInvestment(body));
    }
}
