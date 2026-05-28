package com.antifakenews.dto;

/**
 * Opciones del flujo oficial de procesamiento de noticias (Subfase B).
 *
 * - enrichWithAi: ejecutar enriquecimiento estructurado con IA.
 * - calculateRisk: recalcular el riskScore oficial (servicio determinístico).
 * - persist: persistir en Neo4j (false = solo preview, sin guardar).
 * - replacePreviousAiEnrichment: reemplazar datos AI_ENRICHMENT previos de la noticia.
 */
public record NewsProcessingOptions(
        boolean enrichWithAi,
        boolean calculateRisk,
        boolean persist,
        boolean replacePreviousAiEnrichment
) {
    /** Noticia nueva desde URL: persiste, enriquece (si la IA está disponible) y calcula riesgo. */
    public static NewsProcessingOptions forSubmittedUrl(boolean aiAvailable) {
        return new NewsProcessingOptions(aiAvailable, true, true, false);
    }

    /** Reproceso de noticia existente: reemplaza enriquecimiento IA previo y recalcula. */
    public static NewsProcessingOptions forReprocess() {
        return new NewsProcessingOptions(true, true, true, true);
    }
}
