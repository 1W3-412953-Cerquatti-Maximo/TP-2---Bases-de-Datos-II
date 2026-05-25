package com.antifakenews.service;

import com.antifakenews.dto.TopicDto;
import com.antifakenews.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public List<TopicDto> listAll(String userId) {
        return topicRepository.findAll(userId);
    }
}
