package com.antifakenews.service;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;
import com.antifakenews.dto.CredibilityDiagnosisDto;
import com.antifakenews.dto.EvaluateLinkRequest;
import com.antifakenews.dto.EvaluateLinkResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NewsLinkEvaluationService {

    private final WebArticleExtractionService extractionService;
    private final PreliminaryCredibilityService preliminaryCredibilityService;
    private final AiAnalysisPort aiAnalysisPort;

    public NewsLinkEvaluationService(WebArticleExtractionService extractionService,
                                     PreliminaryCredibilityService preliminaryCredibilityService,
                                     AiAnalysisPort aiAnalysisPort) {
        this.extractionService = extractionService;
        this.preliminaryCredibilityService = preliminaryCredibilityService;
        this.aiAnalysisPort = aiAnalysisPort;
    }

    public EvaluateLinkResponse evaluateLink(EvaluateLinkRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Ingresá una URL válida.");
        }

        WebArticleExtractionService.ExtractedArticle article = extractionService.extract(request.url());
        AiAnalyzeNewsResponse aiAnalysis = aiAnalysisPort.analyze(new AiAnalyzeNewsRequest(article.title(), article.content()));
        CredibilityDiagnosisDto credibilityDiagnosis = preliminaryCredibilityService.diagnose(article);

        List<String> warnings = new ArrayList<>(article.warnings());
        if (!aiAnalysis.enabled()) {
            warnings.add("El resumen asistido por IA no está disponible actualmente; el diagnóstico principal sigue basándose en reglas locales del sistema.");
        }

        return new EvaluateLinkResponse(
                article.originalUrl(),
                article.resolvedUrl(),
                article.title(),
                article.contentPreview(),
                article.fetchStatus(),
                aiAnalysis,
                credibilityDiagnosis,
                List.copyOf(warnings)
        );
    }
}
