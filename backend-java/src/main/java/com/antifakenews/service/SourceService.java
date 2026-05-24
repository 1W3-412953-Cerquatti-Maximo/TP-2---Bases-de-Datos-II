package com.antifakenews.service;

import com.antifakenews.dto.SourceDto;
import com.antifakenews.repository.SourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public List<SourceDto> listAll() {
        return sourceRepository.findAll();
    }
}
