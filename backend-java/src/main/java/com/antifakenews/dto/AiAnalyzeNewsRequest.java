package com.antifakenews.dto;

public record AiAnalyzeNewsRequest(
        String title,
        String content
) {}
