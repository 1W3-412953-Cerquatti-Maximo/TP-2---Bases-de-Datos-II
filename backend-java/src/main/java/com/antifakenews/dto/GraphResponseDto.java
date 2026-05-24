package com.antifakenews.dto;

import java.util.List;

public record GraphResponseDto(
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {}
