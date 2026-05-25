package com.antifakenews.dto;

import java.util.List;

public record EvaluateLinkResponse(
        String url,
        String resolvedUrl,
        String title,
        String contentPreview,
        String fetchStatus,
        AiAnalyzeNewsResponse aiAnalysis,
        CredibilityDiagnosisDto credibilityDiagnosis,
        List<String> warnings
) {}
