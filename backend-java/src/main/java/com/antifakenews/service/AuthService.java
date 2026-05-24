package com.antifakenews.service;

import com.antifakenews.dto.AuthResponse;
import com.antifakenews.dto.CurrentUserDto;
import com.antifakenews.dto.LoginRequest;
import com.antifakenews.dto.RegisterRequest;
import com.antifakenews.exception.ConflictException;
import com.antifakenews.exception.UnauthorizedException;
import com.antifakenews.repository.AuthRepository;
import com.antifakenews.repository.AuthRepository.StoredUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_THEME = "dark";
    private static final long SESSION_TTL_DAYS = 7;
    private static final Set<String> ALLOWED_THEMES = Set.of("dark", "light");

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        validateRegister(request);

        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();

        if (repository.existsByEmail(email)) {
            throw new ConflictException("El email ya está registrado.");
        }
        if (repository.existsByUsername(username)) {
            throw new ConflictException("El nombre de usuario ya está registrado.");
        }

        String id = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(request.password());
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? username
                : request.displayName().trim();

        CurrentUserDto user = repository.createUser(
                id, username, email, displayName, passwordHash, DEFAULT_ROLE, DEFAULT_THEME);

        String token = issueToken(id);
        return new AuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw new UnauthorizedException("Credenciales inválidas.");
        }

        String email = request.email().trim().toLowerCase();
        StoredUser stored = repository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas."));

        if (!passwordEncoder.matches(request.password(), stored.passwordHash())) {
            throw new UnauthorizedException("Credenciales inválidas.");
        }

        String token = issueToken(stored.id());
        CurrentUserDto user = new CurrentUserDto(
                stored.id(), stored.username(), stored.email(),
                stored.displayName(), stored.role(), stored.themePreference());
        return new AuthResponse(token, user);
    }

    public CurrentUserDto me(String token) {
        return resolveUser(token);
    }

    public CurrentUserDto updateThemePreference(String token, String themePreference) {
        CurrentUserDto current = resolveUser(token);

        if (themePreference == null || !ALLOWED_THEMES.contains(themePreference.toLowerCase())) {
            throw new IllegalArgumentException("themePreference debe ser 'dark' o 'light'.");
        }

        return repository.updateThemePreference(current.id(), themePreference.toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Token inválido."));
    }

    public void logout(String token) {
        if (!isBlank(token)) {
            repository.deleteSession(token);
        }
    }

    private CurrentUserDto resolveUser(String token) {
        if (isBlank(token)) {
            throw new UnauthorizedException("Token inválido.");
        }
        return repository.findUserByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Token inválido."));
    }

    private String issueToken(String userId) {
        String token = UUID.randomUUID().toString();
        repository.createSession(userId, token, SESSION_TTL_DAYS);
        return token;
    }

    private void validateRegister(RegisterRequest request) {
        if (request == null
                || isBlank(request.username())
                || isBlank(request.email())
                || isBlank(request.password())) {
            throw new IllegalArgumentException("username, email y password son obligatorios.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
