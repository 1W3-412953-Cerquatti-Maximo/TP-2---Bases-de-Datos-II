package com.antifakenews.controller;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;
import com.antifakenews.security.AuthenticatedUserResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAnalysisPort aiAnalysisPort;
    private final AuthenticatedUserResolver currentUser;

    public AiController(AiAnalysisPort aiAnalysisPort, AuthenticatedUserResolver currentUser) {
        this.aiAnalysisPort = aiAnalysisPort;
        this.currentUser = currentUser;
    }

    @PostMapping("/analyze-news-text")
    public AiAnalyzeNewsResponse analyzeNewsText(@RequestBody AiAnalyzeNewsRequest request) {
        currentUser.requireCurrentUser(); // exige sesión válida
        return aiAnalysisPort.analyze(request);
    }
}
