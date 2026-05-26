package com.antifakenews.dto;

public record GraphSummaryNodeDto(
        String label,
        long count,
        String type
) {}
