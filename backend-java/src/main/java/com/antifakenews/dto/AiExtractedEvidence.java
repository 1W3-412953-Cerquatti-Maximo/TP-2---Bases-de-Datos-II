package com.antifakenews.dto;

/** Señal de evidencia extraída por la IA (Fase IA 3). claimIndex referencia el claim relacionado. */
public record AiExtractedEvidence(
        String text,
        String kind,
        boolean supportsClaim,
        double confidence,
        String explanation,
        int claimIndex
) {}
