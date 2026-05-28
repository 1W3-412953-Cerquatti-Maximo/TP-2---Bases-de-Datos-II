package com.antifakenews.service;

import com.antifakenews.dto.DeleteNewsResponse;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
import com.antifakenews.dto.SubmitNewsUrlRequest;
import com.antifakenews.dto.UrlExtractionDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsRepository;
import com.antifakenews.repository.NewsRepository.DeleteResult;
import com.antifakenews.service.WebArticleExtractionService.BestEffortExtraction;
import org.springframework.stereotype.Service;

import java.net.URI;
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
     * Crea la News básica a partir de una URL enviada por el usuario (extracción
     * best-effort + temas sugeridos + riskScore preliminar del request). Es la
     * pieza de creación reutilizada por {@code NewsProcessingPipelineService};
     * el enriquecimiento IA y el recálculo de riesgo los orquesta el pipeline.
     */
    public CreatedNews createFromUrl(String userId, SubmitNewsUrlRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Ingresá una URL válida.");
        }

        // URL canónica: garantiza que dos envíos equivalentes (trailing slash, host con
        // mayúsculas, fragmento) caigan a la misma cadena para el dedup posterior.
        String normalizedUrl = normalizeUrl(request.url());
        BestEffortExtraction extraction = extractionService.extractMetadata(normalizedUrl);

        String title = firstNonBlank(extraction.title(), request.title(), DEFAULT_TITLE);
        // content = cuerpo COMPLETO extraído (analysisContent) para que la IA no analice solo la vista previa.
        // Fallbacks: lo que mandó el front, la descripción/metadata.
        String content = firstNonBlank(extraction.content(), request.content(), extraction.description(), "");
        String sourceName = firstNonBlank(request.sourceName(), extraction.siteName(), extraction.domain());

        // Fecha de publicación desde metadata si se detectó (fuente METADATA, confianza alta).
        String publishedAtIso = extraction.publishedAtIso();
        String publishedAtSource = publishedAtIso != null ? "METADATA" : "UNKNOWN";
        Double publishedAtConfidence = publishedAtIso != null ? 1.0 : null;

        // El riskScore oficial NO sale del frontend ni de la evaluación preliminar:
        // se crea neutro (PENDING_ANALYSIS) y lo calcula el servicio oficial en el pipeline.
        List<Map<String, Object>> topics = topicSuggestionService.suggest(
                title, content, request.url(), sourceName, request.topicNames());

        var created = newsRepository.createUserSubmittedNews(
                userId,
                UUID.randomUUID().toString(),
                title, content, normalizedUrl,
                UUID.randomUUID().toString(), sourceName, extraction.domain(),
                0L, "LOW", "PENDING_ANALYSIS",
                publishedAtIso, publishedAtSource, publishedAtConfidence,
                topics
        );

        UrlExtractionDto extractionDto = new UrlExtractionDto(
                extraction.success(),
                extraction.titleExtracted(),
                extraction.descriptionExtracted(),
                extraction.warnings()
        );

        return new CreatedNews(
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

    /** Datos de la News recién creada (entrada del pipeline). */
    public record CreatedNews(
            String newsId,
            String title,
            String url,
            String sourceName,
            String status,
            long riskScore,
            String riskLevel,
            List<String> topicNames,
            UrlExtractionDto extraction
    ) {}

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
     * Canónica de la URL para dedup y persistencia: trim, scheme/host a minúsculas,
     * sin slash final, sin fragmento. Mantiene la query string si trae. Si el parsing
     * falla, devuelve sólo el trim original (mejor guardar algo que romper el flujo).
     */
    public static String normalizeUrl(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return "";
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return trimmed;
            String path = uri.getRawPath();
            if (path != null && path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            StringBuilder out = new StringBuilder();
            out.append(scheme.toLowerCase()).append("://").append(host.toLowerCase());
            if (uri.getPort() != -1) out.append(':').append(uri.getPort());
            if (path != null) out.append(path);
            String query = uri.getRawQuery();
            if (query != null && !query.isEmpty()) out.append('?').append(query);
            return out.toString();
        } catch (RuntimeException ex) {
            return trimmed;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
