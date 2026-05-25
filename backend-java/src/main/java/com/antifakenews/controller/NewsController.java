package com.antifakenews.controller;

import com.antifakenews.dto.NewsAnalysisDto;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.NewsAnalysisService;
import com.antifakenews.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final NewsAnalysisService newsAnalysisService;
    private final AuthenticatedUserResolver currentUser;

    public NewsController(NewsService newsService, NewsAnalysisService newsAnalysisService,
                          AuthenticatedUserResolver currentUser) {
        this.newsService = newsService;
        this.newsAnalysisService = newsAnalysisService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NewsSummaryDto> listAll() {
        return newsService.listAll(currentUser.requireUserId());
    }

    @GetMapping("/{id}")
    public NewsDetailDto getById(@PathVariable String id) {
        return newsService.getById(currentUser.requireUserId(), id);
    }

    @GetMapping("/{id}/analysis")
    public NewsAnalysisDto analyze(@PathVariable String id) {
        return newsAnalysisService.analyze(currentUser.requireUserId(), id);
    }
}
