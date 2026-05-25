package com.antifakenews.controller;

import com.antifakenews.dto.SourceDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.SourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;
    private final AuthenticatedUserResolver currentUser;

    public SourceController(SourceService sourceService, AuthenticatedUserResolver currentUser) {
        this.sourceService = sourceService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SourceDto> listAll() {
        return sourceService.listAll(currentUser.requireUserId());
    }
}
