package com.antifakenews.service;

import com.antifakenews.dto.GraphResponseDto;
import com.antifakenews.exception.NotFoundException;
import com.antifakenews.repository.GraphRepository;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    private final GraphRepository graphRepository;

    public GraphService(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    public GraphResponseDto getNewsGraph(String id) {
        return graphRepository.getNewsGraph(id)
                .orElseThrow(() -> new NotFoundException("News not found: " + id));
    }
}
