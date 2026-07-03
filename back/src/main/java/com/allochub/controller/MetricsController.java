package com.allochub.controller;

import com.allochub.metrics.MetricsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String metrics() {
        return metricsService.formatPrometheus();
    }
}
