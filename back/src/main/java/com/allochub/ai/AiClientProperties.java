package com.allochub.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allochub.ai")
public record AiClientProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        boolean enabled) {

    public AiClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 1000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 3000;
        }
    }
}
