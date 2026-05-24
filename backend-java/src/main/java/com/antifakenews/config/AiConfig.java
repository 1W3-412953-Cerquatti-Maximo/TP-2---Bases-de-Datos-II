package com.antifakenews.config;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.ai.DisabledAiAnalysisService;
import com.antifakenews.ai.ExternalAiAnalysisService;
import com.antifakenews.ai.MockAiAnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link AiAnalysisPort} según configuración.
 *
 * ai.enabled=false              -> DisabledAiAnalysisService (default)
 * ai.enabled=true, provider=mock     -> MockAiAnalysisService
 * ai.enabled=true, provider=external -> ExternalAiAnalysisService (fallback controlado si faltan credenciales)
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

    @Bean
    public AiAnalysisPort aiAnalysisPort() {
        if (!enabled) {
            return new DisabledAiAnalysisService();
        }
        String selected = provider == null ? "disabled" : provider.trim().toLowerCase();
        return switch (selected) {
            case "mock" -> new MockAiAnalysisService();
            case "external" -> new ExternalAiAnalysisService(externalApiKey, externalEndpoint);
            default -> new DisabledAiAnalysisService();
        };
    }
}
