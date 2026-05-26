package com.antifakenews.dto;

import java.util.List;

/**
 * Resultado best-effort de la extracción de metadata de la URL.
 * Nunca rompe la creación de la noticia: refleja qué se pudo recuperar.
 */
public record UrlExtractionDto(
        boolean success,
        boolean titleExtracted,
        boolean descriptionExtracted,
        List<String> warnings
) {}
