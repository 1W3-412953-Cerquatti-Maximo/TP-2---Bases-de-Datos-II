package com.antifakenews.dto;

public record DashboardSummaryDto(
        long totalNews,
        long highRiskNews,
        long mediumRiskNews,
        long lowRiskNews,
        long totalSources,
        long totalClaims,
        long totalFactChecks,
        long totalPosts,
        long totalUsers
) {}
