package com.antifakenews.controller;

import com.antifakenews.dto.GraphResponseDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;
    private final AuthenticatedUserResolver currentUser;

    public GraphController(GraphService graphService, AuthenticatedUserResolver currentUser) {
        this.graphService = graphService;
        this.currentUser = currentUser;
    }

    @GetMapping("/news/{id}")
    public GraphResponseDto getNewsGraph(@PathVariable String id) {
        return graphService.getNewsGraph(currentUser.requireUserId(), id);
    }
}
