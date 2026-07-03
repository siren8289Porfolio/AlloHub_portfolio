package com.allochub.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allochub.auth")
public record AuthProperties(String operatorToken, String adminToken) {}
