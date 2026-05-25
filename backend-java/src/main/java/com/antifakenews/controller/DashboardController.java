package com.antifakenews.controller;

import com.antifakenews.dto.DashboardSummaryDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserResolver currentUser;

    public DashboardController(DashboardService dashboardService, AuthenticatedUserResolver currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary() {
        return dashboardService.getSummary(currentUser.requireUserId());
    }
}
