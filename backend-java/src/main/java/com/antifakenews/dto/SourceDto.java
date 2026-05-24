package com.antifakenews.dto;

public record SourceDto(
        String id,
        String name,
        String type,
        Double credibilityScore,
        String url
) {}
