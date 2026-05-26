package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * IA simulada con reglas simples locales. No llama a ninguna API externa.
 * Sirve para demostrar el flujo de asistencia sin depender de credenciales.
 */
public class MockAiAnalysisService implements AiAnalysisPort {

    private static final int MAX_SUMMARY_CHARS = 220;
    private static final int MAX_CLAIMS = 3;
    private static final int MIN_SENTENCE_CHARS = 25;

    @Override
    public AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request) {
        String title = request == null || request.title() == null ? "" : request.title().trim();
        String content = request == null || request.content() == null ? "" : request.content().trim();
        String text = (title + " " + content).toLowerCase();

        Set<String> topics = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();

        // --- Reglas de temas ---
        if (text.contains("5g")) {
            topics.add("Ciencia y Tecnología");
            topics.add("Salud");
        }
        if (text.contains("vacuna")) {
            topics.add("Salud");
        }
        if (text.contains("inflación") || text.contains("inflacion")
                || text.contains("economía") || text.contains("economia")
                || text.contains("dólar") || text.contains("dolar")) {
            topics.add("Economía");
        }
        if (text.contains("clima") || text.contains("temperatura") || text.contains("antártida")
                || text.contains("antartida")) {
            topics.add("Cambio Climático");
        }

        // --- Reglas de advertencias ---
        if (text.contains("colapso")) {
            warnings.add("Lenguaje alarmista detectado ('colapso'): verificar si la afirmación está respaldada o es sensacionalista.");
        }
        if (text.contains("cura milagrosa")) {
            warnings.add("Afirmación extraordinaria ('cura milagrosa') sin evidencia: requiere fuentes científicas verificables.");
        }
        if (text.contains("urgente") || text.contains("compartan") || text.contains("compartir urgente")) {
            warnings.add("Llamado a la difusión urgente: patrón frecuente en cadenas de desinformación.");
        }

        List<String> claims = extractClaims(title, content);
        String summary = buildSummary(title, content);

        return new AiAnalyzeNewsResponse(
                true,
                "mock",
                summary,
                claims,
                new ArrayList<>(topics),
                warnings
        );
    }

    private List<String> extractClaims(String title, String content) {
        List<String> claims = new ArrayList<>();
        if (!title.isBlank()) {
            claims.add(title);
        }
        if (!content.isBlank()) {
            String[] sentences = content.split("(?<=[.!?])\\s+");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (trimmed.length() >= MIN_SENTENCE_CHARS && claims.size() < MAX_CLAIMS) {
                    claims.add(trimmed);
                }
            }
        }
        return claims;
    }

    private String buildSummary(String title, String content) {
        String base = !content.isBlank() ? content : title;
        if (base.isBlank()) {
            return "Resumen automático (IA): no se proporcionó texto suficiente.";
        }
        String shortened = base.length() > MAX_SUMMARY_CHARS
                ? base.substring(0, MAX_SUMMARY_CHARS).trim() + "…"
                : base;
        return "Resumen automático (IA): " + shortened;
    }
}
