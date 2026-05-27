package com.antifakenews.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Respuesta del Asistente IA (capa de explicación textual, NO fuente de verdad).
 *
 * aiRiskLevel/aiRiskScore son una estimación de la IA y NO reemplazan el
 * riskLevel/riskScore oficiales del sistema (determinísticos sobre el grafo).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAnalyzeNewsResponse(
        boolean enabled,
        boolean ok,
        String provider,
        String model,
        String message,
        String summary,
        String riskAnalysis,
        String aiRiskLevel,
        int aiRiskScore,
        List<String> warningSignals,
        List<String> recommendations,
        List<String> limitations,
        double confidence,
        AiUsage usage
) {}
