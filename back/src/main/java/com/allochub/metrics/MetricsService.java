package com.allochub.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public String formatPrometheus() {
        double requests = meterRegistry.find("http.server.requests").counters().stream()
                .mapToDouble(c -> c.count())
                .sum();
        double errors = meterRegistry.find("http.server.requests")
                .tag("status", "500")
                .counters()
                .stream()
                .mapToDouble(c -> c.count())
                .sum();
        double errorRate = requests == 0 ? 0 : errors / requests;

        return """
                # HELP allochub_http_requests_total Total HTTP requests
                # TYPE allochub_http_requests_total counter
                allochub_http_requests_total %s
                # HELP allochub_error_rate_ratio Ratio of 5xx to total
                # TYPE allochub_error_rate_ratio gauge
                allochub_error_rate_ratio %s
                """.formatted(requests, String.format("%.4f", errorRate));
    }
}
