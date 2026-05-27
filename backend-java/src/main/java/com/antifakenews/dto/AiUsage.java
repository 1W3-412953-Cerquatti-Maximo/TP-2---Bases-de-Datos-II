package com.antifakenews.dto;

/** Uso de tokens reportado por el proveedor IA (no contiene datos sensibles). */
public record AiUsage(int inputTokens, int outputTokens) {}
