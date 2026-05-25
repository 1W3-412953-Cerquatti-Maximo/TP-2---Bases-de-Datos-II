package com.antifakenews.controller;

import com.antifakenews.dto.TopicDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.TopicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final AuthenticatedUserResolver currentUser;

    public TopicController(TopicService topicService, AuthenticatedUserResolver currentUser) {
        this.topicService = topicService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TopicDto> listAll() {
        return topicService.listAll(currentUser.requireUserId());
    }
}
