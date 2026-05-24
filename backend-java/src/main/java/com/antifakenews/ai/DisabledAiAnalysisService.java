package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

import java.util.List;

/**
 * Comportamiento por defecto: IA apagada.
 * Devuelve una respuesta controlada (enabled=false) sin tocar ninguna API externa.
 */
public class DisabledAiAnalysisService implements AiAnalysisPort {

    @Override
    public AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request) {
        return new AiAnalyzeNewsResponse(
                false,
                "disabled",
                "El asistente de IA está desactivado. El análisis de riesgo se realiza de forma determinística sobre el grafo Neo4j.",
                List.of(),
                List.of(),
                List.of()
        );
    }
}
