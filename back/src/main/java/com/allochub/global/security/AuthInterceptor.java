package com.allochub.global.security;

import com.allochub.global.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_USER_ATTR = "authUser";

    private final AuthProperties authProperties;

    public AuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isPublic(path)) {
            return true;
        }

        AuthUser user = authenticate(request);
        if (user == null) {
            throw AppException.unauthorized();
        }

        if (path.startsWith("/api/audit-logs") && user.role() != UserRole.admin) {
            throw AppException.forbidden();
        }

        request.setAttribute(AUTH_USER_ATTR, user);
        return true;
    }

    private boolean isPublic(String path) {
        return path.equals("/api/health") || path.equals("/api/metrics");
    }

    private AuthUser authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.equals(authProperties.adminToken())) {
            return new AuthUser("admin", UserRole.admin);
        }
        if (token.equals(authProperties.operatorToken())) {
            return new AuthUser("operator", UserRole.operator);
        }
        return null;
    }

    public static AuthUser requireUser(HttpServletRequest request) {
        Object user = request.getAttribute(AUTH_USER_ATTR);
        if (user instanceof AuthUser authUser) {
            return authUser;
        }
        throw AppException.unauthorized();
    }
}
