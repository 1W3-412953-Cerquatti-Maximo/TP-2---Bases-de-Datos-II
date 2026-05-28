package com.antifakenews.service;

import com.antifakenews.dto.AiNewsEnrichmentResponse;
import com.antifakenews.dto.NewsAnalysisDto;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsProcessingOptions;
import com.antifakenews.dto.NewsProcessingResult;
import com.antifakenews.dto.SubmitNewsUrlRequest;
import com.antifakenews.dto.SubmitNewsUrlResponse;
import com.antifakenews.dto.UrlExtractionDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsRepository;
import com.antifakenews.repository.NewsRepository.ExistingNewsByUrl;
import com.antifakenews.service.NewsService.CreatedNews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Flujo OFICIAL y único de procesamiento de noticias (Subfase B).
 *
 * Centraliza: crear/validar News -> enriquecer con IA (si corresponde) ->
 * recalcular el riskScore con el servicio determinístico oficial -> devolver un
 * resultado unificado. Reutiliza los servicios existentes (no duplica lógica de
 * IA, persistencia ni riesgo). Si la IA falla, la noticia NO se pierde.
 */
@Service
public class NewsProcessingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(NewsProcessingPipelineService.class);

    private final NewsService newsService;
    private final AiNewsEnrichmentService aiEnrichmentService;
    private final NewsAnalysisService newsAnalysisService;
    private final NewsRepository newsRepository;
    private final boolean aiEnabled;
    private final String aiProvider;

    public NewsProcessingPipelineService(NewsService newsService,
                                         AiNewsEnrichmentService aiEnrichmentService,
                                         NewsAnalysisService newsAnalysisService,
                                         NewsRepository newsRepository,
                                         @Value("${ai.enabled:false}") boolean aiEnabled,
                                         @Value("${ai.provider:disabled}") String aiProvider) {
        this.newsService = newsService;
        this.aiEnrichmentService = aiEnrichmentService;
        this.newsAnalysisService = newsAnalysisService;
        this.newsRepository = newsRepository;
        this.aiEnabled = aiEnabled;
        this.aiProvider = aiProvider == null ? "" : aiProvider.trim().toLowerCase();
    }

    /** A) Noticia nueva desde URL: crea, enriquece (best-effort) y calcula riesgo. */
    public SubmitNewsUrlResponse processSubmittedUrl(String userId, SubmitNewsUrlRequest request) {
        NewsProcessingOptions options = NewsProcessingOptions.forSubmittedUrl(aiAvailable());
        log.info("Pipeline submit-url: inicio (enrichWithAi={})", options.enrichWithAi());

        // Dedup por URL: si el usuario ya guardó esta URL, no duplicar ni gastar IA
        // ni recalcular riesgo. Devolvemos la News existente con alreadyExists=true.
        if (request != null && request.url() != null && !request.url().isBlank()) {
            String normalizedUrl = NewsService.normalizeUrl(request.url());
            Optional<ExistingNewsByUrl> existing = newsRepository.findIdByUserAndUrl(userId, normalizedUrl);
            if (existing.isPresent()) {
                ExistingNewsByUrl e = existing.get();
                log.info("Pipeline submit-url: URL ya guardada id={} url={}", e.newsId(), normalizedUrl);
                return new SubmitNewsUrlResponse(
                        e.newsId(), e.title(), e.url(), e.sourceName(), e.status(),
                        e.riskScore(), e.riskLevel(), e.topicNames(),
                        new UrlExtractionDto(true, false, false, List.of()),
                        "SKIPPED", 0, 0, 0, 0, List.of(),
                        true, "Esta noticia ya fue guardada anteriormente."
                );
            }
        }

        CreatedNews created = newsService.createFromUrl(userId, request);
        log.info("Pipeline submit-url: News creada id={}", created.newsId());

        List<String> warnings = new ArrayList<>();
        EnrichOutcome enrich = options.enrichWithAi()
                ? doEnrich(userId, created.newsId())
                : EnrichOutcome.skipped();
        warnings.addAll(enrich.warnings());

        // Riesgo OFICIAL: siempre se calcula con el servicio oficial sobre el grafo
        // (fuente, fact checks, claims/evidencias, propagación), no con el score del
        // frontend ni con la estimación preliminar. Si falla, queda el default neutro.
        long riskScore = created.riskScore();
        String riskLevel = created.riskLevel();
        String status = created.status();
        if (options.calculateRisk()) {
            RiskOutcome risk = recalcRisk(userId, created.newsId(), warnings);
            if (risk != null) {
                riskScore = risk.score();
                riskLevel = risk.level();
                status = "ANALYZED";
            }
        }

        NewsProcessingResult result = new NewsProcessingResult(
                created.newsId(), created.title(), created.url(), created.sourceName(), null,
                riskScore, riskLevel, enrich.status(),
                enrich.topics(), enrich.claims(), enrich.evidences(), enrich.factChecks(),
                List.copyOf(warnings), true, false);
        log.info("Pipeline submit-url: fin id={} status={} risk={}/{} (oficial)",
                result.newsId(), result.aiEnrichmentStatus(), result.riskScore(), result.riskLevel());

        return new SubmitNewsUrlResponse(
                created.newsId(), created.title(), created.url(), created.sourceName(), status,
                riskScore, riskLevel, created.topicNames(), created.extraction(),
                result.aiEnrichmentStatus(), result.topicsCount(), result.claimsCount(),
                result.evidencesCount(), result.factChecksCount(), result.warnings(),
                false, null);
    }

    /** B) Reproceso de noticia existente: valida pertenencia, reemplaza enriquecimiento IA y recalcula. */
    public AiNewsEnrichmentResponse processExistingNews(String userId, String newsId) {
        NewsProcessingOptions options = NewsProcessingOptions.forReprocess();
        // Valida pertenencia (404 si no es del usuario) antes de gastar tokens.
        NewsDetailDto news = newsRepository.findById(userId, newsId)
                .orElseThrow(() -> new NotFoundException("News not found: " + newsId));
        log.info("Pipeline reproceso: inicio id={}", news.id());

        AiNewsEnrichmentResponse enrichment = aiEnrichmentService.enrich(userId, newsId);

        // Reproceso explícito: recalculamos riesgo oficial sobre el grafo completo.
        if (options.calculateRisk() && enrichment.enriched()) {
            List<String> ignored = new ArrayList<>();
            recalcRisk(userId, newsId, ignored);
        }
        log.info("Pipeline reproceso: fin id={} status={}", newsId, enrichment.status());

        return enrichment;
    }

    // ----------------------------- helpers -----------------------------

    private boolean aiAvailable() {
        return aiEnabled && "anthropic".equals(aiProvider);
    }

    /** Enriquecimiento best-effort: nunca tira (salvo 404 de pertenencia), la noticia no se pierde. */
    private EnrichOutcome doEnrich(String userId, String newsId) {
        if (!aiAvailable()) {
            return EnrichOutcome.skipped();
        }
        try {
            AiNewsEnrichmentResponse enr = aiEnrichmentService.enrich(userId, newsId);
            if (enr.enriched()) {
                // Si hubo warnings (truncado, datos parciales, temas ignorados, etc.) marcamos
                // COMPLETED_WITH_WARNINGS: los datos se guardaron, pero hay aclaraciones.
                String status = enr.warnings() != null && !enr.warnings().isEmpty()
                        ? "COMPLETED_WITH_WARNINGS"
                        : "COMPLETED";
                log.info("Pipeline: enriquecimiento IA {} id={} (t={},c={},e={},f={})",
                        status, newsId, enr.topicsCreated(), enr.claimsCreated(),
                        enr.evidencesCreated(), enr.factChecksCreated());
                return new EnrichOutcome(status, enr.topicsCreated(), enr.claimsCreated(),
                        enr.evidencesCreated(), enr.factChecksCreated(), new ArrayList<>(enr.warnings()));
            }
            log.info("Pipeline: enriquecimiento IA no aplicado id={} ({})", newsId, enr.message());
            List<String> warnings = new ArrayList<>();
            if (enr.message() != null && !enr.message().isBlank()) {
                warnings.add(enr.message());
            }
            return new EnrichOutcome("FAILED", 0, 0, 0, 0, warnings);
        } catch (NotFoundException nf) {
            throw nf;
        } catch (RuntimeException ex) {
            log.warn("Pipeline: enriquecimiento IA falló id={}: {}", newsId, ex.getMessage());
            List<String> warnings = new ArrayList<>();
            warnings.add("El enriquecimiento con IA falló; la noticia se guardó sin datos estructurados de IA.");
            return new EnrichOutcome("FAILED", 0, 0, 0, 0, warnings);
        }
    }

    /** Recalcula y persiste el riskScore con el servicio oficial (determinístico sobre el grafo). */
    private RiskOutcome recalcRisk(String userId, String newsId, List<String> warnings) {
        try {
            NewsAnalysisDto analysis = newsAnalysisService.analyze(userId, newsId);
            log.info("Pipeline: riesgo recalculado id={} -> {}/{}", newsId, analysis.riskScore(), analysis.riskLevel());
            return new RiskOutcome(analysis.riskScore(), analysis.riskLevel());
        } catch (RuntimeException ex) {
            log.warn("Pipeline: no se pudo recalcular el riesgo id={}: {}", newsId, ex.getMessage());
            warnings.add("No se pudo recalcular automáticamente el riesgo; se mantiene el valor previo.");
            return null;
        }
    }

    private record EnrichOutcome(String status, int topics, int claims, int evidences, int factChecks,
                                 List<String> warnings) {
        static EnrichOutcome skipped() {
            return new EnrichOutcome("SKIPPED", 0, 0, 0, 0, new ArrayList<>());
        }
    }

    private record RiskOutcome(long score, String level) {}
}
