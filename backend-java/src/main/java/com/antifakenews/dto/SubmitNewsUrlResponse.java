package com.antifakenews.dto;

import java.util.List;

public record SubmitNewsUrlResponse(
        String newsId,
        String title,
        String url,
        String sourceName,
        String status,
        long riskScore,
        String riskLevel,
        List<String> topicNames,
        UrlExtractionDto extraction,
        // Subfase B: resultado del pipeline (procesamiento + enriquecimiento IA).
        String aiEnrichmentStatus,
        int topicsCount,
        int claimsCount,
        int evidencesCount,
        int factChecksCount,
        List<String> warnings,
        // Dedup por URL: cuando true, no se crea una nueva News y se devuelven
        // los datos de la existente para que el frontend ofrezca "Ver noticia".
        boolean alreadyExists,
        String message
) {}
