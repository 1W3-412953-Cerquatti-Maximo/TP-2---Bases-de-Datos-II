package com.antifakenews.controller;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAnalysisPort aiAnalysisPort;

    public AiController(AiAnalysisPort aiAnalysisPort) {
        this.aiAnalysisPort = aiAnalysisPort;
    }

    @PostMapping("/analyze-news-text")
    public AiAnalyzeNewsResponse analyzeNewsText(@RequestBody AiAnalyzeNewsRequest request) {
        return aiAnalysisPort.analyze(request);
    }
}
