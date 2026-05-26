package com.antifakenews.dto;

import java.util.List;

public record SubmitNewsUrlResponse(
        String newsId,
        String title,
        String url,
        String sourceName,
        String status,
        long riskScore,
        String riskLevel,
        List<String> topicNames,
        UrlExtractionDto extraction
) {}
