package com.antifakenews.controller;

import com.antifakenews.dto.DashboardSummaryDto;
import com.antifakenews.dto.GraphSummaryDto;
import com.antifakenews.dto.NewsTimelineDto;
import com.antifakenews.dto.RiskSignalSummaryDto;
import com.antifakenews.dto.TopicRiskRankingDto;
import com.antifakenews.security.AuthenticatedUserResolver;
import com.antifakenews.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/topic-risk-ranking")
    public List<TopicRiskRankingDto> getTopicRiskRanking() {
        return dashboardService.getTopicRiskRanking(currentUser.requireUserId());
    }

    @GetMapping("/risk-signals")
    public List<RiskSignalSummaryDto> getRiskSignals() {
        return dashboardService.getRiskSignals(currentUser.requireUserId());
    }

    @GetMapping("/news-timeline")
    public List<NewsTimelineDto> getNewsTimeline() {
        return dashboardService.getNewsTimeline(currentUser.requireUserId());
    }

    @GetMapping("/graph-summary")
    public GraphSummaryDto getGraphSummary() {
        return dashboardService.getGraphSummary(currentUser.requireUserId());
    }
}
