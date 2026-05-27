import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { NewsAnalysis } from '../models/analysis.model';
import { EvaluateLinkRequest, EvaluateLinkResponse } from '../models/link-evaluation.model';
import { NewsDetail, NewsSummary } from '../models/news.model';
import { DeleteNewsResponse, SubmitNewsUrlRequest, SubmitNewsUrlResponse } from '../models/submit-news.model';
import { AiNewsEnrichmentResponse } from '../models/ai.model';

@Injectable({ providedIn: 'root' })
export class NewsService {
  private http = inject(HttpClient);

  list(): Observable<NewsSummary[]> {
    return this.http.get<NewsSummary[]>(`${API_BASE_URL}/news`);
  }

  getById(id: string): Observable<NewsDetail> {
    return this.http.get<NewsDetail>(`${API_BASE_URL}/news/${id}`);
  }

  analyze(id: string): Observable<NewsAnalysis> {
    return this.http.get<NewsAnalysis>(`${API_BASE_URL}/news/${id}/analysis`);
  }

  evaluateLink(request: EvaluateLinkRequest): Observable<EvaluateLinkResponse> {
    return this.http.post<EvaluateLinkResponse>(`${API_BASE_URL}/news/evaluate-link`, request);
  }

  submitNewsUrl(request: SubmitNewsUrlRequest): Observable<SubmitNewsUrlResponse> {
    return this.http.post<SubmitNewsUrlResponse>(`${API_BASE_URL}/news/submit-url`, request);
  }

  delete(id: string): Observable<DeleteNewsResponse> {
    return this.http.delete<DeleteNewsResponse>(`${API_BASE_URL}/news/${id}`);
  }

  /** Fase IA 3: enriquecimiento estructurado con IA (persiste en Neo4j). */
  enrichWithAi(id: string): Observable<AiNewsEnrichmentResponse> {
    return this.http.post<AiNewsEnrichmentResponse>(`${API_BASE_URL}/news/${id}/ai-enrichment`, {});
  }
}
