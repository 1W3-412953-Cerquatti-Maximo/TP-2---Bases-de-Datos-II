package com.antifakenews.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Resultado unificado del flujo de procesamiento de una noticia (Subfase B).
 * Fuente de verdad reutilizable; los endpoints mapean a sus DTOs específicos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewsProcessingResult(
        String newsId,
        String title,
        String url,
        String sourceName,
        String publishedAt,
        long riskScore,
        String riskLevel,
        String aiEnrichmentStatus,
        int topicsCount,
        int claimsCount,
        int evidencesCount,
        int factChecksCount,
        List<String> warnings,
        boolean created,
        boolean updated
) {}
