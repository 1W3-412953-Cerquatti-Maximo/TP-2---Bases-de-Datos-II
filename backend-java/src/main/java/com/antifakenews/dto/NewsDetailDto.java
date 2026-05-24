package com.antifakenews.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record NewsDetailDto(
        String id,
        String title,
        String content,
        String url,
        ZonedDateTime publishedAt,
        String status,
        Long riskScore,
        String riskLevel,
        SourceDto source,
        List<TopicDto> topics,
        List<ClaimDto> claims,
        List<EvidenceDto> evidence,
        List<FactCheckDto> factChecks,
        List<PostDto> posts,
        List<UserDto> users
) {}
