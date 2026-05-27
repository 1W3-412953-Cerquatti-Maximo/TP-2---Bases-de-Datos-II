package com.antifakenews.dto;

import java.util.List;

/**
 * Pedido al Asistente IA. Mínimo: title + content. El resto es opcional y, si
 * está, se incluye en el prompt como contexto (los datos de riesgo son del
 * sistema y la IA no debe copiarlos sin analizarlos).
 */
public record AiAnalyzeNewsRequest(
        String title,
        String content,
        String url,
        String sourceName,
        List<String> topicNames,
        Integer riskScore,
        String riskLevel
) {
    /** Compatibilidad: permite construir solo con título y contenido. */
    public AiAnalyzeNewsRequest(String title, String content) {
        this(title, content, null, null, null, null, null);
    }
}
