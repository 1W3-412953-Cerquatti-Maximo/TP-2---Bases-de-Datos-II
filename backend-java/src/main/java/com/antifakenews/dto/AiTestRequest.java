package com.antifakenews.dto;

/** Cuerpo opcional para POST /api/ai/test. Si prompt es nulo/vacío se usa uno por defecto. */
public record AiTestRequest(String prompt) {}
