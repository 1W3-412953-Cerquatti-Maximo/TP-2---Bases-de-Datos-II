package com.antifakenews.service;

import com.antifakenews.dto.CredibilityDiagnosisDto;
import com.antifakenews.dto.EvaluateLinkRequest;
import com.antifakenews.dto.EvaluateLinkResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Evaluación principal de un link: extracción web + diagnóstico de credibilidad
 * determinístico. NO ejecuta IA: el "Resumen asistido por IA" es una acción
 * aparte y opcional (POST /api/ai/analyze-news-text), para no consumir tokens
 * sin que el usuario lo pida.
 */
@Service
public class NewsLinkEvaluationService {

    private final WebArticleExtractionService extractionService;
    private final PreliminaryCredibilityService preliminaryCredibilityService;

    public NewsLinkEvaluationService(WebArticleExtractionService extractionService,
                                     PreliminaryCredibilityService preliminaryCredibilityService) {
        this.extractionService = extractionService;
        this.preliminaryCredibilityService = preliminaryCredibilityService;
    }

    public EvaluateLinkResponse evaluateLink(EvaluateLinkRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Ingresá una URL válida.");
        }

        WebArticleExtractionService.ExtractedArticle article = extractionService.extract(request.url());
        CredibilityDiagnosisDto credibilityDiagnosis = preliminaryCredibilityService.diagnose(article);

        return new EvaluateLinkResponse(
                article.originalUrl(),
                article.resolvedUrl(),
                article.title(),
                article.contentPreview(),
                article.fetchStatus(),
                credibilityDiagnosis,
                List.copyOf(article.warnings())
        );
    }
}
