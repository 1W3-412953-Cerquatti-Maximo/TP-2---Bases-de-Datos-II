package com.antifakenews.dto;

import java.util.List;

public record CredibilityDiagnosisDto(
        int riskScore,
        String riskLevel,
        String summary,
        List<RiskSignalDto> signals,
        boolean preliminary,
        String basis
) {}
