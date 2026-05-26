package com.antifakenews.dto;

import java.util.List;

public record SubmitNewsUrlRequest(
        String url,
        String title,
        String content,
        String sourceName,
        List<String> topicNames,
        Integer riskScore,
        String riskLevel,
        String evaluationSummary
) {}
