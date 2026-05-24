package com.antifakenews.dto;

import java.util.Map;

public record GraphEdgeDto(
        String from,
        String to,
        String type,
        Map<String, Object> properties
) {}
