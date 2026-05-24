package com.antifakenews.dto;

import java.util.List;

public record NewsAnalysisDto(
        String newsId,
        String title,
        int riskScore,
        String riskLevel,
        String summary,
        List<RiskSignalDto> signals
) {}
