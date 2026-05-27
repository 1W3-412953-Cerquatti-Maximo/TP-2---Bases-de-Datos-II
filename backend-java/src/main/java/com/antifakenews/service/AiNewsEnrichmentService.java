package com.antifakenews.service;

import com.antifakenews.ai.AnthropicClient;
import com.antifakenews.dto.AiNewsEnrichmentResponse;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.SourceDto;
import com.antifakenews.dto.TopicDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsEnrichmentRepository;
import com.antifakenews.repository.NewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fase IA 3: usa Anthropic para extraer datos estructurados (temas, claims,
 * evidencias, verificaciones asistidas, fecha) del texto de una noticia y los
 * guarda en Neo4j marcados como origin='AI_ENRICHMENT'. No verifica internet,
 * no inventa fuentes externas y no reemplaza el riskScore oficial.
 */
@Service
public class AiNewsEnrichmentService {

    private static final int MAX_ITEMS = 5;
    private static final int MIN_CONTENT_FOR_DEEP = 200;
    private static final double PUBLISHED_AT_MIN_CONFIDENCE = 0.6;

    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> EVIDENCE_KINDS = Set.of(
            "SUPPORTING_EVIDENCE", "REFUTING_EVIDENCE", "MISSING_EVIDENCE", "WEAK_EVIDENCE", "CONTEXTUAL_EVIDENCE");
    private static final Set<String> VERDICTS = Set.of(
            "TRUE", "MOSTLY_TRUE", "MIXED", "MISLEADING", "FALSE", "UNVERIFIED", "REQUIRES_VERIFICATION");

    private final AnthropicClient client;
    private final ObjectMapper mapper;
    private final NewsRepository newsRepository;
    private final NewsEnrichmentRepository enrichmentRepository;
    private final boolean aiEnabled;
    private final String aiProvider;
    private final int maxTokens;
    private final int contentLimit;

    public AiNewsEnrichmentService(AnthropicClient client, ObjectMapper mapper,
                                   NewsRepository newsRepository, NewsEnrichmentRepository enrichmentRepository,
                                   @Value("${ai.enabled:false}") boolean aiEnabled,
                                   @Value("${ai.provider:disabled}") String aiProvider,
                                   @Value("${ai.anthropic.enrichment-max-tokens:2000}") int maxTokens,
                                   @Value("${ai.anthropic.enrichment-content-limit:16000}") int contentLimit) {
        this.client = client;
        this.mapper = mapper;
        this.newsRepository = newsRepository;
        this.enrichmentRepository = enrichmentRepository;
        this.aiEnabled = aiEnabled;
        this.aiProvider = aiProvider == null ? "" : aiProvider.trim().toLowerCase();
        this.maxTokens = maxTokens;
        this.contentLimit = contentLimit;
    }

    public AiNewsEnrichmentResponse enrich(String userId, String newsId) {
        // Pertenencia + contexto (lanza 404 si no existe o no es del usuario).
        NewsDetailDto news = newsRepository.findById(userId, newsId)
                .orElseThrow(() -> new NotFoundException("News not found: " + newsId));

        // Gate de configuración (no marca la noticia: no hubo intento real).
        if (!aiEnabled || !"anthropic".equals(aiProvider)) {
            return failed(newsId, "El enriquecimiento con IA requiere AI_ENABLED=true y AI_PROVIDER=anthropic.");
        }
        if (!client.isConfigured()) {
            return failed(newsId, "ANTHROPIC_API_KEY no está configurada.");
        }

        List<String> warnings = new ArrayList<>();
        String title = safe(news.title());
        String rawContent = safe(news.content());
        if (rawContent.length() < MIN_CONTENT_FOR_DEEP) {
            warnings.add("Contenido insuficiente para enriquecimiento profundo; el análisis se basa en el texto disponible.");
        }
        boolean truncated = rawContent.length() > contentLimit;
        String content = truncated ? rawContent.substring(0, contentLimit) : rawContent;
        if (truncated) {
            warnings.add("El contenido fue truncado para el análisis.");
        }

        AnthropicClient.Result result = client.call(buildPrompt(news, title, content, truncated), maxTokens);
        if (!result.ok()) {
            enrichmentRepository.markFailed(userId, newsId, result.error());
            return failed(newsId, result.error());
        }

        JsonNode json = parseJson(result.text());
        if (json == null) {
            enrichmentRepository.markFailed(userId, newsId, "Respuesta de IA no parseable.");
            return failed(newsId, "No se pudo interpretar la respuesta de la IA (no devolvió JSON válido).");
        }

        // --- Validación + armado de datos a persistir ---
        List<Map<String, Object>> topics = extractTopics(json.path("topics"));

        List<Map<String, Object>> claims = new ArrayList<>();
        List<String> claimIds = new ArrayList<>();
        extractClaims(json.path("claims"), claims, claimIds);

        List<Map<String, Object>> evidences = new ArrayList<>();
        List<Map<String, Object>> factChecks = new ArrayList<>();
        if (!claimIds.isEmpty()) {
            evidences = extractEvidences(json.path("evidences"), claimIds);
            factChecks = extractFactChecks(json.path("factChecks"), claimIds);
        } else if (json.path("evidences").size() > 0 || json.path("factChecks").size() > 0) {
            warnings.add("No se detectaron claims; no se generaron evidencias ni verificaciones.");
        }

        for (JsonNode w : json.path("warnings")) {
            String text = w.asText("").trim();
            if (!text.isEmpty()) warnings.add(text);
        }

        // publishedAt: solo si es válida y con confianza suficiente.
        String publishedAtIso = parsePublishedAt(json.path("publishedAt").asText(null));
        boolean wantUpdatePublishedAt = publishedAtIso != null
                && clamp01(json.path("publishedAtConfidence").asDouble(0.0)) >= PUBLISHED_AT_MIN_CONFIDENCE;

        var persisted = enrichmentRepository.persist(
                userId, newsId, wantUpdatePublishedAt, publishedAtIso,
                "anthropic", client.model(), topics, claims, evidences, factChecks
        ).orElseThrow(() -> new NotFoundException("News not found: " + newsId));

        return new AiNewsEnrichmentResponse(
                newsId, true, "COMPLETED", null,
                persisted.publishedAtUpdated(),
                persisted.topicsCreated(), persisted.claimsCreated(),
                persisted.evidencesCreated(), persisted.factChecksCreated(),
                "anthropic", client.model(), warnings
        );
    }

    // ----------------------------- extracción -----------------------------

    private List<Map<String, Object>> extractTopics(JsonNode node) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!node.isArray()) return list;
        for (JsonNode t : node) {
            if (list.size() >= MAX_ITEMS) break;
            String name = t.path("name").asText("").trim();
            if (name.isEmpty()) continue;
            String slug = t.path("slug").asText("").trim();
            if (slug.isEmpty()) slug = name.toLowerCase().replace(' ', '-');
            list.add(Map.of(
                    "name", name,
                    "slug", slug,
                    "relevance", clamp01(t.path("relevance").asDouble(0.5)),
                    "confidence", clamp01(t.path("confidence").asDouble(0.5))
            ));
        }
        return list;
    }

    private void extractClaims(JsonNode node, List<Map<String, Object>> out, List<String> idsOut) {
        if (!node.isArray()) return;
        for (JsonNode c : node) {
            if (out.size() >= MAX_ITEMS) break;
            String text = c.path("text").asText("").trim();
            if (text.isEmpty()) continue;
            String id = id("claim-ai-");
            String level = c.path("riskLevel").asText("").trim().toUpperCase();
            if (!RISK_LEVELS.contains(level)) level = "MEDIUM";
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", id);
            m.put("text", text);
            m.put("type", blankToDefault(c.path("type").asText(""), "general"));
            m.put("riskLevel", level);
            m.put("confidence", clamp01(c.path("confidence").asDouble(0.5)));
            m.put("explanation", c.path("explanation").asText("").trim());
            out.add(m);
            idsOut.add(id);
        }
    }

    private List<Map<String, Object>> extractEvidences(JsonNode node, List<String> claimIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!node.isArray()) return list;
        for (JsonNode e : node) {
            if (list.size() >= MAX_ITEMS) break;
            String text = e.path("text").asText("").trim();
            if (text.isEmpty()) continue;
            String kind = e.path("kind").asText("").trim().toUpperCase();
            if (!EVIDENCE_KINDS.contains(kind)) kind = "MISSING_EVIDENCE";
            int idx = clampIndex(e.path("claimIndex").asInt(0), claimIds.size());
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", id("evid-ai-"));
            m.put("claimId", claimIds.get(idx));
            m.put("relType", relTypeFor(kind));
            m.put("description", text);
            m.put("kind", kind);
            m.put("confidence", clamp01(e.path("confidence").asDouble(0.5)));
            m.put("explanation", e.path("explanation").asText("").trim());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> extractFactChecks(JsonNode node, List<String> claimIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!node.isArray()) return list;
        for (JsonNode f : node) {
            if (list.size() >= MAX_ITEMS) break;
            String verdict = f.path("verdict").asText("").trim().toUpperCase();
            if (!VERDICTS.contains(verdict)) verdict = "REQUIRES_VERIFICATION";
            int idx = clampIndex(f.path("claimIndex").asInt(0), claimIds.size());
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", id("fc-ai-"));
            m.put("claimId", claimIds.get(idx));
            m.put("verdict", verdict);
            m.put("explanation", f.path("explanation").asText("").trim());
            m.put("confidence", clamp01(f.path("confidence").asDouble(0.5)));
            list.add(m);
        }
        return list;
    }

    private String relTypeFor(String kind) {
        return switch (kind) {
            case "REFUTING_EVIDENCE" -> "REFUTED_BY";
            case "MISSING_EVIDENCE", "WEAK_EVIDENCE" -> "HAS_EVIDENCE_GAP";
            default -> "SUPPORTED_BY";
        };
    }

    // ----------------------------- prompt -----------------------------

    private String buildPrompt(NewsDetailDto news, String title, String content, boolean truncated) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sos un asistente de análisis de desinformación de NexoVeraz. ")
                .append("Extraé datos estructurados ÚNICAMENTE a partir del texto de la noticia. ")
                .append("Reglas: no inventes fuentes externas, no afirmes haber verificado internet en tiempo real, ")
                .append("no determines verdad absoluta. Si no podés determinar algo, devolvé null o un array vacío. ")
                .append("Los 'factChecks' son verificaciones ASISTIDAS basadas en el texto, no fact-checking externo real; ")
                .append("si no hay evidencia suficiente usá verdict REQUIRES_VERIFICATION o UNVERIFIED. ")
                .append("La fecha de publicación solo devolvela si está explícita o claramente inferible del texto.\n\n");

        sb.append("Noticia:\n");
        sb.append("- Título: ").append(title.isBlank() ? "(sin título)" : title).append("\n");
        if (truncated) sb.append("- (El contenido fue truncado)\n");
        sb.append("- Contenido: ").append(content.isBlank() ? "(sin contenido)" : content).append("\n");
        if (news.url() != null && !news.url().isBlank()) sb.append("- URL: ").append(news.url()).append("\n");
        SourceDto source = news.source();
        if (source != null && source.name() != null) {
            sb.append("- Fuente: ").append(source.name());
            if (source.credibilityScore() != null) sb.append(" (credibilidad ").append(source.credibilityScore()).append(")");
            sb.append("\n");
        }
        List<TopicDto> topics = news.topics();
        if (topics != null && !topics.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (TopicDto t : topics) if (t.name() != null) names.add(t.name());
            if (!names.isEmpty()) sb.append("- Temas actuales: ").append(String.join(", ", names)).append("\n");
        }
        if (news.riskScore() != null || news.riskLevel() != null) {
            sb.append("- Riesgo del sistema (referencia, NO lo copies sin analizar): score=")
                    .append(news.riskScore() == null ? "?" : news.riskScore())
                    .append(", nivel=").append(news.riskLevel() == null ? "?" : news.riskLevel()).append("\n");
        }

        sb.append("\nRespondé ÚNICAMENTE con JSON válido en español (sin markdown, sin ```), con estas claves:\n");
        sb.append("""
                {
                  "publishedAt": "ISO-8601 o null",
                  "publishedAtConfidence": 0.0,
                  "topics": [{"name":"", "slug":"", "relevance":0.0, "confidence":0.0}],
                  "claims": [{"text":"", "type":"", "riskLevel":"LOW|MEDIUM|HIGH", "confidence":0.0, "explanation":""}],
                  "evidences": [{"text":"", "kind":"SUPPORTING_EVIDENCE|REFUTING_EVIDENCE|MISSING_EVIDENCE|WEAK_EVIDENCE|CONTEXTUAL_EVIDENCE", "supportsClaim":false, "confidence":0.0, "explanation":"", "claimIndex":0}],
                  "factChecks": [{"verdict":"TRUE|MOSTLY_TRUE|MIXED|MISLEADING|FALSE|UNVERIFIED|REQUIRES_VERIFICATION", "confidence":0.0, "explanation":"", "claimIndex":0}],
                  "warnings": [""]
                }
                """);
        sb.append("claimIndex es el índice (0-based) del claim relacionado dentro de \"claims\". ")
                .append("Máximo 5 elementos por lista. confidence/relevance entre 0 y 1.");
        return sb.toString();
    }

    // ----------------------------- helpers -----------------------------

    private JsonNode parseJson(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl >= 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        try {
            return mapper.readTree(t);
        } catch (Exception ignored) {
            // fallback por llaves
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

    /** Devuelve el ISO si es parseable; si no, null (no se actualiza publishedAt). */
    private String parsePublishedAt(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) return null;
        String v = value.trim();
        try {
            OffsetDateTime.parse(v);
            return v;
        } catch (Exception ignored) {
            // probamos sin offset
        }
        try {
            LocalDateTime.parse(v);
            return v + "Z";
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiNewsEnrichmentResponse failed(String newsId, String message) {
        return new AiNewsEnrichmentResponse(newsId, false, "FAILED", message,
                false, 0, 0, 0, 0, "anthropic", client.model(), List.of());
    }

    private double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    private int clampIndex(int idx, int size) {
        if (size <= 0) return 0;
        if (idx < 0) return 0;
        return Math.min(idx, size - 1);
    }

    private String blankToDefault(String value, String def) {
        return value == null || value.isBlank() ? def : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }
}
