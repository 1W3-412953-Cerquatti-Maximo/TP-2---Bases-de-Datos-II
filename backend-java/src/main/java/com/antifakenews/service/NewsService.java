package com.antifakenews.service;

import com.antifakenews.dto.DeleteNewsResponse;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
import com.antifakenews.dto.SubmitNewsUrlRequest;
import com.antifakenews.dto.SubmitNewsUrlResponse;
import com.antifakenews.dto.UrlExtractionDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsRepository;
import com.antifakenews.repository.NewsRepository.DeleteResult;
import com.antifakenews.service.WebArticleExtractionService.BestEffortExtraction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsService {

    private static final String DEFAULT_TITLE = "Noticia enviada por URL";

    private final NewsRepository newsRepository;
    private final WebArticleExtractionService extractionService;
    private final TopicSuggestionService topicSuggestionService;

    public NewsService(NewsRepository newsRepository, WebArticleExtractionService extractionService,
                       TopicSuggestionService topicSuggestionService) {
        this.newsRepository = newsRepository;
        this.extractionService = extractionService;
        this.topicSuggestionService = topicSuggestionService;
    }

    public List<NewsSummaryDto> listAll(String userId) {
        return newsRepository.findAll(userId);
    }

    public NewsDetailDto getById(String userId, String id) {
        return newsRepository.findById(userId, id)
                .orElseThrow(() -> new NotFoundException("News not found: " + id));
    }

    /**
     * Crea una noticia a partir de una URL enviada por el usuario.
     * Intenta extraer metadata (best-effort) y completa con los valores manuales
     * del request cuando faltan. Persiste el riskScore evaluado si vino en el
     * request y asocia temas (provistos + sugeridos automáticamente).
     */
    public SubmitNewsUrlResponse submitUrl(String userId, SubmitNewsUrlRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Ingresá una URL válida.");
        }

        BestEffortExtraction extraction = extractionService.extractMetadata(request.url().trim());

        String title = firstNonBlank(extraction.title(), request.title(), DEFAULT_TITLE);
        String content = firstNonBlank(request.content(), extraction.description(), "");
        String sourceName = firstNonBlank(request.sourceName(), extraction.siteName(), extraction.domain());

        RiskOutcome risk = resolveRisk(request.riskScore(), request.riskLevel());
        List<Map<String, Object>> topics = topicSuggestionService.suggest(
                title, content, request.url(), sourceName, request.topicNames());

        var created = newsRepository.createUserSubmittedNews(
                userId,
                UUID.randomUUID().toString(),
                title, content, request.url().trim(),
                UUID.randomUUID().toString(), sourceName, extraction.domain(),
                risk.score(), risk.level(), risk.status(),
                topics
        );

        UrlExtractionDto extractionDto = new UrlExtractionDto(
                extraction.success(),
                extraction.titleExtracted(),
                extraction.descriptionExtracted(),
                extraction.warnings()
        );

        return new SubmitNewsUrlResponse(
                created.newsId(),
                created.title(),
                created.url(),
                created.sourceName(),
                created.status(),
                created.riskScore(),
                created.riskLevel(),
                created.topicNames(),
                extractionDto
        );
    }

    /** Elimina una noticia del usuario. Lanza 404 si no existe o no le pertenece. */
    public DeleteNewsResponse deleteNews(String userId, String newsId) {
        DeleteResult result = newsRepository.deleteOwnedNews(userId, newsId)
                .orElseThrow(() -> new NotFoundException("News not found: " + newsId));
        return new DeleteNewsResponse(
                result.newsId(),
                result.deleted(),
                result.sourceDeleted(),
                result.sourceName()
        );
    }

    /**
     * Determina score/level/status persistidos.
     * Con riskScore presente: clampea 0–100, valida o deriva el nivel y marca EVALUATED.
     * Sin riskScore: 0 / LOW / PENDING_ANALYSIS.
     */
    private RiskOutcome resolveRisk(Integer riskScore, String riskLevel) {
        if (riskScore == null) {
            return new RiskOutcome(0, "LOW", "PENDING_ANALYSIS");
        }
        long score = Math.max(0, Math.min(100, riskScore));
        String level = normalizeLevel(riskLevel, score);
        return new RiskOutcome(score, level, "EVALUATED");
    }

    private String normalizeLevel(String riskLevel, long score) {
        if (riskLevel != null) {
            String upper = riskLevel.trim().toUpperCase();
            if (upper.equals("LOW") || upper.equals("MEDIUM") || upper.equals("HIGH")) {
                return upper;
            }
        }
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private record RiskOutcome(long score, String level, String status) {}

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
