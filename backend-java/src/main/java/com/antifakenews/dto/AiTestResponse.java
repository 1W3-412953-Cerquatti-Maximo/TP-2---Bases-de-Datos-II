package com.antifakenews.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Respuesta del diagnóstico de conexión IA (GET /api/ai/health, POST /api/ai/test).
 *
 * - enabled: si la IA está activada (ai.enabled).
 * - provider: proveedor seleccionado ("mock" | "anthropic").
 * - model: modelo configurado (cuando aplica).
 * - configured: si el proveedor tiene lo necesario para operar (ej. API key).
 * - ok: si la prueba de conexión fue exitosa.
 * - message: mensaje legible (nunca incluye secretos ni stacktrace).
 * - usage: tokens usados, si el proveedor los reporta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiTestResponse(
        boolean enabled,
        String provider,
        String model,
        boolean configured,
        boolean ok,
        String message,
        AiUsage usage
) {}
