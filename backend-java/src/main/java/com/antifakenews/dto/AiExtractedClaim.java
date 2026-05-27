package com.antifakenews.dto;

/** Afirmación (claim) extraída por la IA (Fase IA 3). */
public record AiExtractedClaim(
        String text,
        String type,
        String riskLevel,
        double confidence,
        String explanation
) {}
