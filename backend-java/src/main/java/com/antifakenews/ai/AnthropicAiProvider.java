package com.antifakenews.ai;

import com.antifakenews.dto.AiTestResponse;

/**
 * Proveedor real Anthropic para la PRUEBA DE CONEXIÓN (Fase IA 1).
 * Delega la llamada HTTP en {@link AnthropicClient} (sin duplicar lógica).
 */
public class AnthropicAiProvider implements AiProvider {

    private final AnthropicClient client;

    public AnthropicAiProvider(AnthropicClient client) {
        this.client = client;
    }

    @Override
    public String providerName() {
        return "anthropic";
    }

    @Override
    public AiTestResponse testConnection(String prompt) {
        if (!client.isConfigured()) {
            return new AiTestResponse(true, "anthropic", client.model(), false, false,
                    "ANTHROPIC_API_KEY no está configurada.", null);
        }

        AnthropicClient.Result result = client.call(prompt);
        if (result.ok()) {
            String message = (result.text() == null || result.text().isBlank())
                    ? "La conexión IA de NexoVeraz funciona correctamente."
                    : result.text().trim();
            return new AiTestResponse(true, "anthropic", client.model(), true, true, message, result.usage());
        }
        return new AiTestResponse(true, "anthropic", client.model(), true, false, result.error(), null);
    }
}
