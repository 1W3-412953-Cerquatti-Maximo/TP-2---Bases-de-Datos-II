package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

import java.util.List;

/**
 * Estructura preparada para un futuro proveedor de IA externo (API REST).
 *
 * No depende de ninguna API real ni obliga a tener credenciales. Si faltan
 * credenciales, falla de forma CONTROLADA (enabled=false) en lugar de romper.
 * La llamada real se implementará en una fase futura sin tocar el resto del sistema.
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
        if (apiKey == null || apiKey.isBlank()) {
            return new AiAnalyzeNewsResponse(
                    false,
                    "external",
                    "Proveedor externo seleccionado pero sin credenciales configuradas. IA desactivada de forma controlada.",
                    List.of(),
                    List.of(),
                    List.of("Configurá AI_EXTERNAL_API_KEY (y AI_EXTERNAL_ENDPOINT) para habilitar el proveedor externo.")
            );
        }

        // Punto de extensión: acá iría la llamada HTTP real al proveedor externo
        // usando apiKey + endpoint. Se deja preparado sin implementar para no
        // depender de una API concreta ni de credenciales reales.
        return new AiAnalyzeNewsResponse(
                false,
                "external",
                "Integración con proveedor externo aún no implementada. La estructura está lista para conectarse en una fase futura.",
                List.of(),
                List.of(),
                List.of("La conexión real con el proveedor externo se implementará más adelante; el análisis determinístico del grafo no se ve afectado.")
        );
    }
}
