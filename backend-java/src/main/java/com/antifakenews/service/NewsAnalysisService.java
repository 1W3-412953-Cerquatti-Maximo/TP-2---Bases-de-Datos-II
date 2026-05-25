package com.antifakenews.service;

import com.antifakenews.dto.NewsAnalysisDto;
import com.antifakenews.dto.RiskSignalDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsAnalysisRepository;
import com.antifakenews.repository.NewsAnalysisRepository.AnalysisInputs;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsAnalysisService {

    private static final double LOW_CREDIBILITY_THRESHOLD = 0.4;
    private static final long   POST_VOLUME_THRESHOLD     = 3;
    private static final long   HIGH_REACH_THRESHOLD      = 20_000;

    private static final int MEDIUM_LEVEL_MIN = 40;
    private static final int HIGH_LEVEL_MIN   = 70;
    private static final int MAX_SCORE        = 100;

    private static final Map<String, String> REASON_PHRASES = Map.of(
            "LOW_CREDIBILITY_SOURCE",       "proviene de una fuente de baja confiabilidad",
            "FALSE_FACT_CHECK",             "contiene afirmaciones marcadas como falsas por fact-check",
            "MISLEADING_FACT_CHECK",        "contiene afirmaciones marcadas como engañosas por fact-check",
            "CLAIM_REFUTED_BY_EVIDENCE",    "contiene afirmaciones refutadas por evidencia",
            "CLAIM_WITHOUT_EVIDENCE",       "tiene afirmaciones sin evidencia que las respalde ni refute",
            "HIGH_PROPAGATION_VOLUME",      "tiene un volumen alto de publicaciones que la difunden",
            "CONNECTED_USERS_PROPAGATION",  "fue difundida por usuarios conectados entre sí",
            "HIGH_REACH_POST",              "fue amplificada por publicaciones de alto alcance"
    );

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

        repository.persistRisk(id, finalScore, level);

        return new NewsAnalysisDto(id, inputs.title(), finalScore, level, summary, signals);
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
