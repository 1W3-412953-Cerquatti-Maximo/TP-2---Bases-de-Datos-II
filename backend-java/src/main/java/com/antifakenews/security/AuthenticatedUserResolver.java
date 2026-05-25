package com.antifakenews.security;

import com.antifakenews.dto.CurrentUserDto;
import com.antifakenews.exception.UnauthorizedException;
import com.antifakenews.repository.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Helper reutilizable de autenticación para endpoints protegidos.
 *
 * Lee el header Authorization de la request actual, valida el formato Bearer
 * y resuelve el usuario por token. Si falta o es inválido, lanza
 * UnauthorizedException (401). No usa la cadena de filtros de Spring Security.
 */
@Component
public class AuthenticatedUserResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthRepository authRepository;
    private final HttpServletRequest request;

    public AuthenticatedUserResolver(AuthRepository authRepository, HttpServletRequest request) {
        this.authRepository = authRepository;
        this.request = request;
    }

    public CurrentUserDto requireCurrentUser() {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Falta el header Authorization: Bearer <token>.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Token vacío.");
        }
        return authRepository.findUserByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Token inválido o expirado."));
    }

    public String requireUserId() {
        return requireCurrentUser().id();
    }
}
