package com.antifakenews.dto;

public record DeleteNewsResponse(
        String newsId,
        boolean deleted,
        boolean sourceDeleted,
        String sourceName
) {}
