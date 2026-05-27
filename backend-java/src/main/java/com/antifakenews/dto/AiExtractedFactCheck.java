package com.antifakenews.dto;

/** Verificación asistida extraída por la IA (Fase IA 3). claimIndex referencia el claim verificado. */
public record AiExtractedFactCheck(
        String verdict,
        double confidence,
        String explanation,
        int claimIndex
) {}
