package com.antifakenews.dto;

public record DashboardSummaryDto(
        long totalNews,
        long highRiskNews,
        long mediumRiskNews,
        long lowRiskNews,
        long totalSources,
        long highCredibilitySources,
        long mediumCredibilitySources,
        long lowCredibilitySources,
        long totalClaims,
        long totalFactChecks,
        long totalPosts,
        long totalUsers
) {}
