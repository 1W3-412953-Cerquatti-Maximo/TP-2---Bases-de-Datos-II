package com.antifakenews.dto;

import java.util.List;

public record AiAnalyzeNewsResponse(
        boolean enabled,
        String provider,
        String summary,
        List<String> suggestedClaims,
        List<String> suggestedTopics,
        List<String> warnings
) {}
