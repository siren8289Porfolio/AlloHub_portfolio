package com.allochub.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP client for FastAPI AI Service. Failures are isolated here so ledger
 * transactions are never rolled back because of AI unavailability.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient aiRestClient;
    private final AiClientProperties properties;

    public AiClient(RestClient aiRestClient, AiClientProperties properties) {
        this.aiRestClient = aiRestClient;
        this.properties = properties;
    }

    public Map<String, Object> queryEvidence(String companyName, String investmentId) {
        if (!properties.enabled()) {
            return unavailableEvidence("AI client disabled");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("companyName", companyName);
            body.put("investmentId", investmentId);
            Map<String, Object> response = aiRestClient
                    .post()
                    .uri("/ai/evidence/query")
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null || response.isEmpty()) {
                return unavailableEvidence("empty evidence response");
            }
            response.putIfAbsent("available", Boolean.TRUE);
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("FastAPI evidence query unavailable: {}", ex.toString());
            return unavailableEvidence(ex.getMessage());
        }
    }

    public Map<String, Object> explain(
            String companyName,
            String investmentId,
            Map<String, Object> internalContext,
            List<Map<String, Object>> evidence) {
        if (!properties.enabled()) {
            return unavailableExplanation("AI client disabled");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("companyName", companyName);
            body.put("investmentId", investmentId);
            body.put("internalContext", internalContext);
            body.put("evidence", evidence);
            Map<String, Object> response = aiRestClient
                    .post()
                    .uri("/ai/explain")
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null || response.isEmpty()) {
                return unavailableExplanation("empty explanation response");
            }
            response.putIfAbsent("available", Boolean.TRUE);
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("FastAPI explain unavailable: {}", ex.toString());
            return unavailableExplanation(ex.getMessage());
        }
    }

    private Map<String, Object> unavailableEvidence(String reason) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UNAVAILABLE");
        response.put("available", false);
        response.put("reason", reason == null ? "unknown" : reason);
        response.put(
                "evidence",
                List.of(Map.of(
                        "source", "FastAPI AI Service",
                        "scope", "external evidence",
                        "status", "UNAVAILABLE",
                        "message", "evidence unavailable: graceful degradation")));
        return response;
    }

    private Map<String, Object> unavailableExplanation(String reason) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UNAVAILABLE");
        response.put("available", false);
        response.put("reason", reason == null ? "unknown" : reason);
        response.put("explanation", "explanation unavailable");
        response.put("grounded", false);
        response.put(
                "caveats",
                List.of(
                        "FastAPI AI Service 호출에 실패했습니다.",
                        "내부 원장 facts는 Spring Backend에서 계속 제공됩니다.",
                        "원장 트랜잭션은 AI 장애의 영향을 받지 않습니다."));
        return response;
    }
}
