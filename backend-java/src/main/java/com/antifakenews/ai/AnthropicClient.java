package com.antifakenews.ai;

import com.antifakenews.dto.AiUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente de bajo nivel para la Messages API de Anthropic (Claude), con el
 * HttpClient nativo de Java 21 (sin SDK). Reutilizable por el health-check
 * (Fase IA 1) y por el Asistente IA (Fase IA 2).
 *
 * Seguridad:
 * - La API key llega por configuración; nunca se hardcodea ni se loguea.
 * - Los errores se devuelven como {@link Result} controlado (nunca stacktrace).
 */
@Component
public class AnthropicClient {

    private static final String MESSAGES_PATH = "/v1/messages";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final String version;
    private final int defaultMaxTokens;
    private final int timeoutSeconds;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public AnthropicClient(
            @Value("${ai.anthropic.api-key:}") String apiKey,
            @Value("${ai.anthropic.model:claude-haiku-4-5}") String model,
            @Value("${ai.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${ai.anthropic.version:2023-06-01}") String version,
            @Value("${ai.anthropic.max-tokens:512}") int defaultMaxTokens,
            @Value("${ai.anthropic.timeout-seconds:25}") int timeoutSeconds,
            ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.version = version;
        this.defaultMaxTokens = defaultMaxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 15)))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public Result call(String userPrompt) {
        return call(userPrompt, defaultMaxTokens);
    }

    public Result call(String userPrompt, int maxTokens) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + MESSAGES_PATH))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", version)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                JsonNode root = mapper.readTree(response.body());
                return new Result(true, status, extractText(root), extractUsage(root), null);
            }
            return new Result(false, status, null, null, mapError(status, response.body()));

        } catch (HttpTimeoutException e) {
            return new Result(false, 0, null, null, "La conexión con Anthropic excedió el tiempo de espera (timeout).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, 0, null, null, "La conexión con Anthropic fue interrumpida.");
        } catch (Exception e) {
            return new Result(false, 0, null, null, "No se pudo conectar con Anthropic.");
        }
    }

    private String mapError(int status, String body) {
        String base = switch (status) {
            case 401, 403 -> "Error de autenticación con Anthropic (revisá ANTHROPIC_API_KEY).";
            case 429 -> "Anthropic devolvió límite de uso (rate limit, 429). Probá nuevamente más tarde.";
            case 400 -> "Request inválido a Anthropic (revisá ANTHROPIC_MODEL o el cuerpo).";
            default -> status >= 500
                    ? "Anthropic tuvo un error temporal (" + status + "). Probá más tarde."
                    : "Anthropic devolvió un error (" + status + ").";
        };
        String detail = safeProviderMessage(body);
        return detail == null ? base : base + " Detalle: " + detail;
    }

    private String safeProviderMessage(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode msg = mapper.readTree(body).path("error").path("message");
            if (msg.isTextual() && !msg.asText().isBlank()) {
                String text = msg.asText().trim();
                return text.length() > 200 ? text.substring(0, 200) + "…" : text;
            }
        } catch (Exception ignored) {
            // cuerpo no-JSON: no exponemos nada
        }
        return null;
    }

    private String extractText(JsonNode root) {
        JsonNode content = root.path("content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText(null);
                }
            }
        }
        return null;
    }

    private AiUsage extractUsage(JsonNode root) {
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode()) return null;
        return new AiUsage(usage.path("input_tokens").asInt(0), usage.path("output_tokens").asInt(0));
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return "https://api.anthropic.com";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Resultado controlado de una llamada a Anthropic. */
    public record Result(boolean ok, int status, String text, AiUsage usage, String error) {}
}
