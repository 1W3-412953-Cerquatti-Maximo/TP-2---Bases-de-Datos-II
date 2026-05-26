package com.antifakenews.dto;

public record TopicRiskRankingDto(
        String topic,
        long avgRiskScore,
        long newsCount
) {}
