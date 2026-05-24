package com.antifakenews.controller;

import com.antifakenews.dto.NewsAnalysisDto;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
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

    public NewsController(NewsService newsService, NewsAnalysisService newsAnalysisService) {
        this.newsService = newsService;
        this.newsAnalysisService = newsAnalysisService;
    }

    @GetMapping
    public List<NewsSummaryDto> listAll() {
        return newsService.listAll();
    }

    @GetMapping("/{id}")
    public NewsDetailDto getById(@PathVariable String id) {
        return newsService.getById(id);
    }

    @GetMapping("/{id}/analysis")
    public NewsAnalysisDto analyze(@PathVariable String id) {
        return newsAnalysisService.analyze(id);
    }
}
