package com.antifakenews.dto;

import java.time.ZonedDateTime;

public record FactCheckDto(
        String id,
        String verdict,
        String explanation,
        Double confidence,
        ZonedDateTime publishedAt
) {}
