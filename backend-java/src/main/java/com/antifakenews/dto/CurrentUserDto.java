package com.antifakenews.dto;

public record CurrentUserDto(
        String id,
        String username,
        String email,
        String displayName,
        String role,
        String themePreference
) {}
