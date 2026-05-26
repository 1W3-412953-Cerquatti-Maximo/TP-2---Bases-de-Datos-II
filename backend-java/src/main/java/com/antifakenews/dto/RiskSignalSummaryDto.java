package com.antifakenews.dto;

public record RiskSignalSummaryDto(
        String code,
        String label,
        long count
) {}
