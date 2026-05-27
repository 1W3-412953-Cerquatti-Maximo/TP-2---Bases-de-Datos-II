import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import {
  DashboardSummary,
  GraphSummaryResponse,
  RiskSignalSummaryItem,
  TopicRiskRankingItem
} from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${API_BASE_URL}/dashboard/summary`);
  }

  getTopicRiskRanking(): Observable<TopicRiskRankingItem[]> {
    return this.http.get<TopicRiskRankingItem[]>(`${API_BASE_URL}/dashboard/topic-risk-ranking`);
  }

  getRiskSignals(): Observable<RiskSignalSummaryItem[]> {
    return this.http.get<RiskSignalSummaryItem[]>(`${API_BASE_URL}/dashboard/risk-signals`);
  }

  getGraphSummary(): Observable<GraphSummaryResponse> {
    return this.http.get<GraphSummaryResponse>(`${API_BASE_URL}/dashboard/graph-summary`);
  }
}
