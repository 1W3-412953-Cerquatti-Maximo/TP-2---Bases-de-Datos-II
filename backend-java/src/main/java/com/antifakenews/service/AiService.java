package com.antifakenews.service;

import com.antifakenews.ai.AiProvider;
import com.antifakenews.dto.AiTestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orquesta el diagnóstico de conexión IA. Si la IA está desactivada
 * (ai.enabled=false) responde sin llamar a ningún proveedor.
 */
@Service
public class AiService {

    private static final String DEFAULT_PROMPT =
            "Respondé en español con una frase corta confirmando que la conexión IA de NexoVeraz funciona.";

    private final boolean enabled;
    private final AiProvider provider;

    public AiService(@Value("${ai.enabled:false}") boolean enabled, AiProvider provider) {
        this.enabled = enabled;
        this.provider = provider;
    }

    public AiTestResponse health() {
        return run(DEFAULT_PROMPT);
    }

    public AiTestResponse test(String prompt) {
        String effective = (prompt == null || prompt.isBlank()) ? DEFAULT_PROMPT : prompt;
        return run(effective);
    }

    private AiTestResponse run(String prompt) {
        if (!enabled) {
            return new AiTestResponse(false, provider.providerName(), null, false, true,
                    "La IA está desactivada.", null);
        }
        return provider.testConnection(prompt);
    }
}
