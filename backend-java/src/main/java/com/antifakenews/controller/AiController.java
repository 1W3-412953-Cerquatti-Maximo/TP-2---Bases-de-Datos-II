package com.antifakenews.controller;

import com.antifakenews.ai.AiAnalysisPort;
import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;
import com.antifakenews.dto.AiTestRequest;
import com.antifakenews.dto.AiTestResponse;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAnalysisPort aiAnalysisPort;
    private final AiService aiService;
    private final AuthenticatedUserResolver currentUser;

    public AiController(AiAnalysisPort aiAnalysisPort, AiService aiService, AuthenticatedUserResolver currentUser) {
        this.aiAnalysisPort = aiAnalysisPort;
        this.aiService = aiService;
        this.currentUser = currentUser;
    }

    @PostMapping("/analyze-news-text")
    public AiAnalyzeNewsResponse analyzeNewsText(@RequestBody AiAnalyzeNewsRequest request) {
        currentUser.requireCurrentUser(); // exige sesión válida
        return aiAnalysisPort.analyze(request);
    }

    /** Diagnóstico de conexión IA (Fase IA 1). Protegido por Bearer token. */
    @GetMapping("/health")
    public AiTestResponse health() {
        currentUser.requireCurrentUser();
        return aiService.health();
    }

    /** Prueba de conexión con prompt opcional. Protegido por Bearer token. */
    @PostMapping("/test")
    public AiTestResponse test(@RequestBody(required = false) AiTestRequest request) {
        currentUser.requireCurrentUser();
        return aiService.test(request == null ? null : request.prompt());
    }
}
