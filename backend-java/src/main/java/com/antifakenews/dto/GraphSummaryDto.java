package com.antifakenews.dto;

import java.util.List;

public record GraphSummaryDto(
        List<GraphSummaryNodeDto> nodes,
        long totalRelationships
) {}
