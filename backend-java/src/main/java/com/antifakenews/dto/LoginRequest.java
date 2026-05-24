package com.antifakenews.dto;

public record LoginRequest(
        String email,
        String password
) {}
