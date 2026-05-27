package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;
import com.antifakenews.dto.AiUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Asistente IA real con Anthropic (Fase IA 2): genera resumen, análisis de
 * riesgo asistido, señales de alerta, recomendaciones y limitaciones a partir
 * del texto recibido.
 *
 * Importante: es una capa de EXPLICACIÓN textual. No crea nodos en Neo4j, no
 * modifica claims/evidencias/fact checks/posts/usuarios ni el riskScore oficial.
 */
public class AnthropicAiAnalysisService implements AiAnalysisPort {

    private final AnthropicClient client;
    private final ObjectMapper mapper;
    private final int maxTokens;

    public AnthropicAiAnalysisService(AnthropicClient client, ObjectMapper mapper, int maxTokens) {
        this.client = client;
        this.mapper = mapper;
        this.maxTokens = maxTokens;
    }

    @Override
    public AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request) {
        if (!client.isConfigured()) {
            return error("ANTHROPIC_API_KEY no está configurada.");
        }

        String title = safe(request == null ? null : request.title());
        String content = safe(request == null ? null : request.content());
        if (title.isBlank() && content.isBlank()) {
            return error("Faltan datos: enviá al menos título o contenido para analizar.");
        }

        AnthropicClient.Result result = client.call(buildPrompt(request, title, content), maxTokens);
        if (!result.ok()) {
            return error(result.error());
        }

        JsonNode json = parseJson(result.text());
        if (json == null) {
            return error("No se pudo interpretar la respuesta de la IA (no devolvió JSON válido).");
        }
        return mapSuccess(json, result.usage());
    }

    private AiAnalyzeNewsResponse mapSuccess(JsonNode json, AiUsage usage) {
        int score = clampScore(json.path("aiRiskScore").asInt(0));
        return new AiAnalyzeNewsResponse(
                true,
                true,
                "anthropic",
                client.model(),
                null,
                text(json, "summary"),
                text(json, "riskAnalysis"),
                normalizeLevel(json.path("aiRiskLevel").asText(""), score),
                score,
                stringList(json.path("warningSignals")),
                stringList(json.path("recommendations")),
                stringList(json.path("limitations")),
                clampConfidence(json.path("confidence").asDouble(0.0)),
                usage
        );
    }

    private AiAnalyzeNewsResponse error(String message) {
        return new AiAnalyzeNewsResponse(true, false, "anthropic", client.model(), message,
                null, null, null, 0, List.of(), List.of(), List.of(), 0.0, null);
    }

    private String buildPrompt(AiAnalyzeNewsRequest r, String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sos un asistente de análisis de desinformación de NexoVeraz. ")
                .append("Analizá ÚNICAMENTE el texto de la noticia que se da a continuación. ")
                .append("Reglas: no determines verdad absoluta, no digas simplemente que la noticia es falsa, ")
                .append("no inventes fuentes externas, no afirmes haber verificado internet en tiempo real, ")
                .append("y señalá la incertidumbre cuando corresponda.\n\n");

        sb.append("Noticia:\n");
        sb.append("- Título: ").append(title.isBlank() ? "(sin título)" : title).append("\n");
        sb.append("- Contenido: ").append(content.isBlank() ? "(sin contenido)" : content).append("\n");
        if (notBlank(r.sourceName())) {
            sb.append("- Fuente declarada: ").append(r.sourceName().trim()).append("\n");
        }
        if (notBlank(r.url())) {
            sb.append("- URL: ").append(r.url().trim()).append("\n");
        }
        if (r.topicNames() != null && !r.topicNames().isEmpty()) {
            sb.append("- Temas del sistema: ").append(String.join(", ", r.topicNames())).append("\n");
        }
        if (r.riskScore() != null || notBlank(r.riskLevel())) {
            sb.append("- Riesgo del SISTEMA (determinístico; es un dato de referencia, NO lo copies sin analizarlo): ")
                    .append("score=").append(r.riskScore() == null ? "?" : r.riskScore())
                    .append(", nivel=").append(notBlank(r.riskLevel()) ? r.riskLevel() : "?").append("\n");
        }

        sb.append("\nRespondé ÚNICAMENTE con un JSON válido en español (sin markdown, sin ```, ")
                .append("sin texto fuera del JSON) con EXACTAMENTE estas claves:\n");
        sb.append("{\n")
                .append("  \"summary\": \"resumen breve y claro de la noticia\",\n")
                .append("  \"riskAnalysis\": \"explicación textual del riesgo de desinformación\",\n")
                .append("  \"aiRiskLevel\": \"LOW | MEDIUM | HIGH\",\n")
                .append("  \"aiRiskScore\": 0,\n")
                .append("  \"warningSignals\": [\"señales de alerta detectadas en el texto\"],\n")
                .append("  \"recommendations\": [\"acciones de revisión sugeridas al usuario\"],\n")
                .append("  \"limitations\": [\"aclaraciones sobre los límites de este análisis\"],\n")
                .append("  \"confidence\": 0.0\n")
                .append("}\n");
        sb.append("aiRiskScore es un entero entre 0 y 100. confidence es un número entre 0 y 1.");
        return sb.toString();
    }

    /** Quita fences ```json y, si hace falta, recorta al primer { … último }. */
    private JsonNode parseJson(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
            t = t.trim();
        }
        try {
            return mapper.readTree(t);
        } catch (Exception ignored) {
            // intento de recorte por llaves
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return mapper.readTree(t.substring(start, end + 1));
            } catch (Exception ignored) {
                // no es JSON
            }
        }
        return null;
    }

    private String normalizeLevel(String level, int score) {
        String up = level == null ? "" : level.trim().toUpperCase();
        if (up.equals("LOW") || up.equals("MEDIUM") || up.equals("HIGH")) {
            return up;
        }
        // Si la IA no devolvió un nivel válido, lo derivamos del score.
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private double clampConfidence(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private List<String> stringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isEmpty()) {
                    list.add(value);
                }
            }
        }
        return list;
    }

    private String text(JsonNode json, String key) {
        String value = json.path(key).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
