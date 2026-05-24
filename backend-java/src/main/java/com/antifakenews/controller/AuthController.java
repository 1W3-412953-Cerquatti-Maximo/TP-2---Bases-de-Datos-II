package com.antifakenews.controller;

import com.antifakenews.dto.AuthResponse;
import com.antifakenews.dto.CurrentUserDto;
import com.antifakenews.dto.LoginRequest;
import com.antifakenews.dto.RegisterRequest;
import com.antifakenews.dto.UpdateUserPreferenceRequest;
import com.antifakenews.exception.UnauthorizedException;
import com.antifakenews.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserDto me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return authService.me(extractToken(authHeader));
    }

    @PutMapping("/me/preferences")
    public CurrentUserDto updatePreferences(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateUserPreferenceRequest request) {
        return authService.updateThemePreference(extractToken(authHeader), request.themePreference());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(extractToken(authHeader));
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Falta el header Authorization: Bearer <token>.");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Token vacío.");
        }
        return token;
    }
}
