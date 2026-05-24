import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { NewsAnalysis } from '../models/analysis.model';
import { NewsDetail, NewsSummary } from '../models/news.model';

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
}
