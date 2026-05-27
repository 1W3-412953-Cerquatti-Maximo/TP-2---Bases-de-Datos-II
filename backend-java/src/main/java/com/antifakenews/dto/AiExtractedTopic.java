package com.antifakenews.dto;

/** Tema extraído por la IA (Fase IA 3). */
public record AiExtractedTopic(
        String name,
        String slug,
        double relevance,
        double confidence
) {}
