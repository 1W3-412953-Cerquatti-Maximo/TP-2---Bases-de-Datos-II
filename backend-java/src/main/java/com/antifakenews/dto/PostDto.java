package com.antifakenews.dto;

import java.time.ZonedDateTime;

public record PostDto(
        String id,
        String content,
        String platform,
        ZonedDateTime createdAt
) {}
