package com.allochub.controller;

import com.allochub.global.exception.AppException;
import com.allochub.global.response.ApiResponse;
import com.allochub.global.security.AuthInterceptor;
import com.allochub.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/session")
    public ApiResponse<Map<String, String>> session(HttpServletRequest request) {
        Object user = request.getAttribute(AuthInterceptor.AUTH_USER_ATTR);
        if (!(user instanceof AuthUser authUser)) {
            throw AppException.unauthorized();
        }
        return ApiResponse.ok(Map.of("user_id", authUser.id(), "role", authUser.role().name()));
    }
}
