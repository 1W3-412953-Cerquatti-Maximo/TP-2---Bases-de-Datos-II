package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

import java.util.List;

/**
 * Placeholder de un proveedor externo genérico (distinto de Anthropic). Para la
 * IA real usar AI_PROVIDER=anthropic ({@link AnthropicAiAnalysisService}).
 * Si se selecciona "external" responde de forma controlada sin romper nada.
 */
public class ExternalAiAnalysisService implements AiAnalysisPort {

    private final String apiKey;
    private final String endpoint;

    public ExternalAiAnalysisService(String apiKey, String endpoint) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request) {
        String message = (apiKey == null || apiKey.isBlank())
                ? "Proveedor externo seleccionado pero sin credenciales. Para IA real usá AI_PROVIDER=anthropic."
                : "Proveedor externo genérico no implementado. Para IA real usá AI_PROVIDER=anthropic.";
        return new AiAnalyzeNewsResponse(false, false, "external", null, message,
                null, null, null, 0, List.of(), List.of(), List.of(), 0.0, null);
    }
}
