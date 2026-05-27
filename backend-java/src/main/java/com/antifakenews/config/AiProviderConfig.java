package com.antifakenews.config;

import com.antifakenews.ai.AiProvider;
import com.antifakenews.ai.AnthropicAiProvider;
import com.antifakenews.ai.AnthropicClient;
import com.antifakenews.ai.MockAiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona el {@link AiProvider} para la prueba de conexión (/api/ai/health):
 *
 * ai.enabled=true + ai.provider=anthropic -> AnthropicAiProvider (llamada real)
 * en cualquier otro caso                   -> MockAiProvider (sin API real)
 */
@Configuration
public class AiProviderConfig {

    @Value("${ai.enabled:false}")
    private boolean enabled;

    @Value("${ai.provider:disabled}")
    private String provider;

    @Bean
    public AiProvider aiProvider(AnthropicClient anthropicClient) {
        String selected = provider == null ? "" : provider.trim().toLowerCase();
        if (enabled && "anthropic".equals(selected)) {
            return new AnthropicAiProvider(anthropicClient);
        }
        return new MockAiProvider();
    }
}
