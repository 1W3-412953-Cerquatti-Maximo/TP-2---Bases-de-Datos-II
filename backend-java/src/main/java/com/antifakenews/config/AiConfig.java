package com.antifakenews.config;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.ai.AnthropicAiAnalysisService;
import com.antifakenews.ai.AnthropicClient;
import com.antifakenews.ai.DisabledAiAnalysisService;
import com.antifakenews.ai.ExternalAiAnalysisService;
import com.antifakenews.ai.MockAiAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link AiAnalysisPort} según configuración.
 *
 * ai.enabled=false                    -> DisabledAiAnalysisService (default)
 * ai.enabled=true, provider=mock      -> MockAiAnalysisService (simulado local)
 * ai.enabled=true, provider=anthropic -> AnthropicAiAnalysisService (IA real)
 * ai.enabled=true, provider=external  -> ExternalAiAnalysisService (placeholder controlado)
 */
@Configuration
public class AiConfig {

    @Value("${ai.enabled:false}")
    private boolean enabled;

    @Value("${ai.provider:disabled}")
    private String provider;

    @Value("${ai.external.api-key:}")
    private String externalApiKey;

    @Value("${ai.external.endpoint:}")
    private String externalEndpoint;

    @Value("${ai.anthropic.analysis-max-tokens:1600}")
    private int analysisMaxTokens;

    @Bean
    public AiAnalysisPort aiAnalysisPort(AnthropicClient anthropicClient, ObjectMapper mapper) {
        if (!enabled) {
            return new DisabledAiAnalysisService();
        }
        String selected = provider == null ? "disabled" : provider.trim().toLowerCase();
        return switch (selected) {
            case "mock" -> new MockAiAnalysisService();
            case "anthropic" -> new AnthropicAiAnalysisService(anthropicClient, mapper, analysisMaxTokens);
            case "external" -> new ExternalAiAnalysisService(externalApiKey, externalEndpoint);
            default -> new DisabledAiAnalysisService();
        };
    }
}
