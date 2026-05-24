package com.antifakenews.dto;

public record RiskSignalDto(
        String code,
        String description,
        int points
) {}
