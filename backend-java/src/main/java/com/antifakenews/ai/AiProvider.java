package com.antifakenews.ai;

import com.antifakenews.dto.AiTestResponse;

/**
 * Proveedor de IA modular para PRUEBA DE CONEXIÓN (Fase IA 1).
 *
 * Solo verifica que se puede llamar al proveedor real y devuelve una respuesta
 * de prueba. NO enriquece noticias ni toca claims/evidencias/fact checks/posts.
 * Implementaciones: {@link MockAiProvider}, {@link AnthropicAiProvider}.
 */
public interface AiProvider {

    /** Nombre del proveedor ("mock" | "anthropic"). */
    String providerName();

    /** Hace una llamada mínima al proveedor y devuelve el resultado controlado. */
    AiTestResponse testConnection(String prompt);
}
