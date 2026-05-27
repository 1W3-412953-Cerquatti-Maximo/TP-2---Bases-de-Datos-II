package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * IA simulada con reglas simples locales (no llama a ninguna API externa).
 * Sirve para demostrar el flujo del Asistente IA sin credenciales. Devuelve la
 * misma estructura que el proveedor real (resumen + análisis + señales + etc.).
 */
public class MockAiAnalysisService implements AiAnalysisPort {

    private static final int MAX_SUMMARY_CHARS = 240;

    @Override
    public AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request) {
        String title = request == null || request.title() == null ? "" : request.title().trim();
        String content = request == null || request.content() == null ? "" : request.content().trim();
        String text = (title + " " + content).toLowerCase();

        List<String> warnings = new ArrayList<>();
        if (text.contains("colapso") || text.contains("urgente") || text.contains("compart")) {
            warnings.add("Lenguaje alarmista o llamado a la difusión urgente: patrón frecuente en cadenas de desinformación.");
        }
        if (text.contains("cura milagrosa") || text.contains("100%") || text.contains("seguro")) {
            warnings.add("Afirmación extraordinaria o absoluta sin matices: requiere fuentes verificables.");
        }
        if (!text.contains("estudio") && !text.contains("fuente") && !text.contains("según")) {
            warnings.add("No se citan estudios, organismos ni fuentes verificables dentro del texto.");
        }

        int score = Math.min(100, 25 + warnings.size() * 20);
        String level = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";

        List<String> recommendations = List.of(
                "Contrastar la afirmación con medios y organismos oficiales.",
                "Buscar estudios o datos primarios citados en el texto.",
                "Revisar la reputación de la fuente antes de compartir."
        );
        List<String> limitations = List.of(
                "Análisis simulado (mock) sobre el texto disponible; no consulta internet ni un modelo real.",
                "No reemplaza el cálculo de riesgo determinístico del sistema ni una verificación externa."
        );

        return new AiAnalyzeNewsResponse(
                true,
                true,
                "mock",
                "mock",
                null,
                buildSummary(title, content),
                "Análisis simulado: se evaluaron señales textuales básicas (lenguaje alarmista, afirmaciones absolutas, ausencia de fuentes). " +
                        "Es una estimación orientativa, no una verificación.",
                level,
                score,
                warnings,
                recommendations,
                limitations,
                0.4,
                null
        );
    }

    private String buildSummary(String title, String content) {
        String base = !content.isBlank() ? content : title;
        if (base.isBlank()) {
            return "No se proporcionó texto suficiente para resumir.";
        }
        return base.length() > MAX_SUMMARY_CHARS
                ? base.substring(0, MAX_SUMMARY_CHARS).trim() + "…"
                : base;
    }
}
