package com.antifakenews.service;

import com.antifakenews.dto.DashboardSummaryDto;
import com.antifakenews.repository.DashboardRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummaryDto getSummary() {
        return dashboardRepository.getSummary();
    }
}
