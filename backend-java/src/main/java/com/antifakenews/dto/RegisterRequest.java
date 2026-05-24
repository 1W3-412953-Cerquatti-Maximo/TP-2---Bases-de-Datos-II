package com.antifakenews.dto;

public record RegisterRequest(
        String username,
        String email,
        String displayName,
        String password
) {}
