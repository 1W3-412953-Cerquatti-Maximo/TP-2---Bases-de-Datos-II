package com.antifakenews.service;

import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public List<NewsSummaryDto> listAll() {
        return newsRepository.findAll();
    }

    public NewsDetailDto getById(String id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("News not found: " + id));
    }
}
