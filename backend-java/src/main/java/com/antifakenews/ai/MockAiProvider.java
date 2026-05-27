package com.antifakenews.ai;

import com.antifakenews.dto.AiTestResponse;

/**
 * Proveedor simulado: no llama a ninguna API real. Útil para desarrollo y demo
 * sin credenciales. Siempre responde ok=true.
 */
public class MockAiProvider implements AiProvider {

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public AiTestResponse testConnection(String prompt) {
        return new AiTestResponse(
                true,
                "mock",
                "mock",
                true,
                true,
                "Proveedor mock activo: respuesta simulada (no se llamó a ninguna API real).",
                null);
    }
}
