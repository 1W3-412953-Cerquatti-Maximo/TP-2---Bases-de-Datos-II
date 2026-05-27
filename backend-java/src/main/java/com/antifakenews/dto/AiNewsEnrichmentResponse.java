package com.antifakenews.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Resultado del enriquecimiento estructurado con IA (Fase IA 3).
 * status: "COMPLETED" | "FAILED". message es nulo en éxito.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiNewsEnrichmentResponse(
        String newsId,
        boolean enriched,
        String status,
        String message,
        boolean publishedAtUpdated,
        int topicsCreated,
        int claimsCreated,
        int evidencesCreated,
        int factChecksCreated,
        String provider,
        String model,
        List<String> warnings
) {}
