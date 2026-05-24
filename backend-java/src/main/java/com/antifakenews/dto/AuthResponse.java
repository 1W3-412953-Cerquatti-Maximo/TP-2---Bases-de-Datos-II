package com.antifakenews.dto;

public record AuthResponse(
        String token,
        CurrentUserDto user
) {}
