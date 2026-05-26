package com.antifakenews.service;

import com.antifakenews.dto.DashboardSummaryDto;
import com.antifakenews.dto.GraphSummaryDto;
import com.antifakenews.dto.NewsTimelineDto;
import com.antifakenews.dto.RiskSignalSummaryDto;
import com.antifakenews.dto.TopicRiskRankingDto;
import com.antifakenews.repository.DashboardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    /** Etiquetas legibles para cada código de señal (orden estable de referencia). */
    private static final Map<String, String> SIGNAL_LABELS = new LinkedHashMap<>();
    static {
        SIGNAL_LABELS.put("LOW_CREDIBILITY_SOURCE", "Fuente de baja confiabilidad");
        SIGNAL_LABELS.put("CLAIM_REFUTED_BY_EVIDENCE", "Claim refutado por evidencia");
        SIGNAL_LABELS.put("FALSE_FACT_CHECK", "Fact-check falso");
        SIGNAL_LABELS.put("MISLEADING_FACT_CHECK", "Fact-check engañoso");
        SIGNAL_LABELS.put("CLAIM_WITHOUT_EVIDENCE", "Claim sin evidencia");
        SIGNAL_LABELS.put("HIGH_PROPAGATION_VOLUME", "Alta propagación");
        SIGNAL_LABELS.put("CONNECTED_USERS_PROPAGATION", "Usuarios conectados difundiendo la misma noticia");
        SIGNAL_LABELS.put("HIGH_REACH_POST", "Post de alto alcance");
    }

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummaryDto getSummary(String userId) {
        return dashboardRepository.getSummary(userId);
    }

    public List<TopicRiskRankingDto> getTopicRiskRanking(String userId) {
        return dashboardRepository.topicRiskRanking(userId);
    }

    /** Señales con count > 0, ordenadas de más a menos frecuentes, top 5. */
    public List<RiskSignalSummaryDto> getRiskSignals(String userId) {
        Map<String, Long> counts = dashboardRepository.riskSignalCounts(userId);
        List<RiskSignalSummaryDto> signals = new ArrayList<>();
        for (Map.Entry<String, String> entry : SIGNAL_LABELS.entrySet()) {
            long count = counts.getOrDefault(entry.getKey(), 0L);
            if (count > 0) {
                signals.add(new RiskSignalSummaryDto(entry.getKey(), entry.getValue(), count));
            }
        }
        signals.sort(Comparator.comparingLong(RiskSignalSummaryDto::count).reversed());
        return signals.size() > 5 ? signals.subList(0, 5) : signals;
    }

    public List<NewsTimelineDto> getNewsTimeline(String userId) {
        return dashboardRepository.newsTimeline(userId);
    }

    public GraphSummaryDto getGraphSummary(String userId) {
        return dashboardRepository.graphSummary(userId);
    }
}
