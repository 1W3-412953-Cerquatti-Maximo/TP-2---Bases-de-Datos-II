package com.antifakenews.service;

import com.antifakenews.dto.NewsAnalysisDto;
import com.antifakenews.dto.RiskSignalDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsAnalysisRepository;
import com.antifakenews.repository.NewsAnalysisRepository.AnalysisInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsAnalysisService {

    private static final double LOW_CREDIBILITY_THRESHOLD = 0.4;
    private static final long   POST_VOLUME_THRESHOLD     = 3;
    private static final long   HIGH_REACH_THRESHOLD      = 20_000;

    private static final int MEDIUM_LEVEL_MIN = 40;
    private static final int HIGH_LEVEL_MIN   = 70;
    private static final int MAX_SCORE        = 100;

    private static final Map<String, String> REASON_PHRASES = Map.ofEntries(
            Map.entry("LOW_CREDIBILITY_SOURCE",       "proviene de una fuente de baja confiabilidad"),
            Map.entry("FALSE_FACT_CHECK",             "contiene afirmaciones marcadas como falsas por fact-check"),
            Map.entry("MISLEADING_FACT_CHECK",        "contiene afirmaciones marcadas como engañosas por fact-check"),
            Map.entry("CLAIM_REFUTED_BY_EVIDENCE",    "contiene afirmaciones refutadas por evidencia"),
            Map.entry("CLAIM_WITHOUT_EVIDENCE",       "tiene afirmaciones sin evidencia que las respalde ni refute"),
            Map.entry("HIGH_RISK_AI_CLAIM",           "incluye afirmaciones detectadas como de alto riesgo (lenguaje absoluto, médico o conspirativo)"),
            Map.entry("MISSING_OR_WEAK_EVIDENCE",     "presenta afirmaciones apoyadas en evidencia faltante o débil"),
            Map.entry("HIGH_PROPAGATION_VOLUME",      "tiene un volumen alto de publicaciones que la difunden"),
            Map.entry("CONNECTED_USERS_PROPAGATION",  "fue difundida por usuarios conectados entre sí"),
            Map.entry("HIGH_REACH_POST",              "fue amplificada por publicaciones de alto alcance")
    );

    private static final Logger log = LoggerFactory.getLogger(NewsAnalysisService.class);

    private final NewsAnalysisRepository repository;

    public NewsAnalysisService(NewsAnalysisRepository repository) {
        this.repository = repository;
    }

    public NewsAnalysisDto analyze(String userId, String id) {
        AnalysisInputs inputs = repository.fetchSignals(userId, id)
                .orElseThrow(() -> new NotFoundException("News not found: " + id));

        List<RiskSignalDto> signals = new ArrayList<>();
        int rawScore = 0;

        if (inputs.sourceCredibility() != null && inputs.sourceCredibility() < LOW_CREDIBILITY_THRESHOLD) {
            signals.add(new RiskSignalDto(
                    "LOW_CREDIBILITY_SOURCE",
                    "La fuente tiene baja confiabilidad histórica (credibilityScore < 0.4).",
                    25));
            rawScore += 25;
        }

        if (inputs.falseChecks() > 0) {
            signals.add(new RiskSignalDto(
                    "FALSE_FACT_CHECK",
                    "Al menos un fact-check asociado emitió veredicto FALSE.",
                    40));
            rawScore += 40;
        }

        if (inputs.misleadingChecks() > 0) {
            signals.add(new RiskSignalDto(
                    "MISLEADING_FACT_CHECK",
                    "Al menos un fact-check asociado emitió veredicto MISLEADING.",
                    25));
            rawScore += 25;
        }

        if (inputs.refutedClaims() > 0) {
            signals.add(new RiskSignalDto(
                    "CLAIM_REFUTED_BY_EVIDENCE",
                    "Al menos un claim de la noticia está refutado por evidencia (REFUTED_BY).",
                    30));
            rawScore += 30;
        }

        if (inputs.claimsWithoutEvidence() > 0) {
            signals.add(new RiskSignalDto(
                    "CLAIM_WITHOUT_EVIDENCE",
                    "Al menos un claim no tiene evidencia que lo respalde ni lo refute.",
                    20));
            rawScore += 20;
        }

        // Señales aportadas por el enriquecimiento IA estructurado.
        // El riskLevel del Claim refleja la fuerza/peligro del lenguaje del texto
        // (médico/causal absoluto, conspirativo) y se persiste por la IA; sumarlo evita
        // que noticias textualmente arriesgadas queden subvaluadas cuando la IA es
        // (correctamente) cautelosa con verdicts FALSE/MISLEADING.
        if (inputs.highRiskClaims() > 0) {
            signals.add(new RiskSignalDto(
                    "HIGH_RISK_AI_CLAIM",
                    "Al menos un claim fue etiquetado riskLevel=HIGH por el enriquecimiento IA.",
                    15));
            rawScore += 15;
        }

        // HAS_EVIDENCE_GAP cubre los kinds MISSING_EVIDENCE y WEAK_EVIDENCE: claims
        // sostenidos por evidencia ausente o débil. Se acumula a CLAIM_WITHOUT_EVIDENCE
        // cuando el claim tiene SOLO ese tipo de evidencia (igual contará en ambas).
        if (inputs.missingOrWeakEvidenceClaims() > 0) {
            signals.add(new RiskSignalDto(
                    "MISSING_OR_WEAK_EVIDENCE",
                    "Al menos un claim está apoyado por evidencia faltante o débil (HAS_EVIDENCE_GAP).",
                    10));
            rawScore += 10;
        }

        if (inputs.postCount() > POST_VOLUME_THRESHOLD) {
            signals.add(new RiskSignalDto(
                    "HIGH_PROPAGATION_VOLUME",
                    "La noticia tiene más de 3 publicaciones que la difunden.",
                    10));
            rawScore += 10;
        }

        if (inputs.connectedPairs() > 0) {
            signals.add(new RiskSignalDto(
                    "CONNECTED_USERS_PROPAGATION",
                    "Existen usuarios conectados (FOLLOWS / INTERACTS_WITH) que difundieron la misma noticia.",
                    15));
            rawScore += 15;
        }

        if (inputs.maxReach() != null && inputs.maxReach() > HIGH_REACH_THRESHOLD) {
            signals.add(new RiskSignalDto(
                    "HIGH_REACH_POST",
                    "Al menos una publicación que la difunde alcanzó más de 20.000 personas.",
                    10));
            rawScore += 10;
        }

        int finalScore = Math.min(rawScore, MAX_SCORE);
        String level = computeLevel(finalScore);
        String summary = buildSummary(level, signals);

        // Desglose para auditoría/diagnóstico: qué señales aportaron y cuánto.
        // No loguea contenido de la noticia; solo conteos del grafo y puntajes.
        logBreakdown(id, inputs, signals, rawScore, finalScore, level);

        repository.persistRisk(id, finalScore, level);

        return new NewsAnalysisDto(id, inputs.title(), finalScore, level, summary, signals);
    }

    private static void logBreakdown(String newsId, AnalysisInputs i, List<RiskSignalDto> signals,
                                     int rawScore, int finalScore, String level) {
        String signalsText = signals.stream()
                .map(s -> s.code() + "(+" + s.points() + ")")
                .collect(Collectors.joining(", "));
        log.info("RiskScore newsId={} inputs[srcCred={} falseFC={} misleadFC={} refuted={} " +
                        "noEvidence={} highRiskClaims={} missingOrWeakEv={} posts={} maxReach={} connected={}] " +
                        "signals=[{}] rawScore={} finalScore={} level={}",
                newsId,
                i.sourceCredibility(), i.falseChecks(), i.misleadingChecks(), i.refutedClaims(),
                i.claimsWithoutEvidence(), i.highRiskClaims(), i.missingOrWeakEvidenceClaims(),
                i.postCount(), i.maxReach(), i.connectedPairs(),
                signalsText, rawScore, finalScore, level);
    }

    private static String computeLevel(int score) {
        if (score >= HIGH_LEVEL_MIN) return "HIGH";
        if (score >= MEDIUM_LEVEL_MIN) return "MEDIUM";
        return "LOW";
    }

    private static String buildSummary(String level, List<RiskSignalDto> signals) {
        String levelText = switch (level) {
            case "HIGH"   -> "alto riesgo";
            case "MEDIUM" -> "riesgo medio";
            default       -> "bajo riesgo";
        };

        if (signals.isEmpty()) {
            return "Esta noticia presenta " + levelText
                    + ": no se detectaron señales de riesgo en el grafo.";
        }

        List<String> phrases = signals.stream()
                .map(s -> REASON_PHRASES.getOrDefault(s.code(), s.code()))
                .toList();

        String reasons;
        if (phrases.size() == 1) {
            reasons = phrases.get(0);
        } else {
            String tail = phrases.get(phrases.size() - 1);
            String head = String.join(", ", phrases.subList(0, phrases.size() - 1));
            reasons = head + " y " + tail;
        }

        return "Esta noticia presenta " + levelText + " porque " + reasons + ".";
    }
}
