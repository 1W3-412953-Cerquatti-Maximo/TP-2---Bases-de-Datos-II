package com.antifakenews.dto;

public record NewsTimelineDto(
        String date,
        long low,
        long medium,
        long high,
        long total
) {}
