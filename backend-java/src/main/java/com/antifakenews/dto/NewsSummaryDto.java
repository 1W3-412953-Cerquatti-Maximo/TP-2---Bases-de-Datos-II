package com.antifakenews.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record NewsSummaryDto(
        String id,
        String title,
        String url,
        ZonedDateTime publishedAt,
        String status,
        Long riskScore,
        String riskLevel,
        String sourceName,
        List<String> topicNames
) {}
