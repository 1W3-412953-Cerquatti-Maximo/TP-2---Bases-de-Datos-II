import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { AiAnalyzeNewsRequest, AiAnalyzeNewsResponse } from '../models/ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);

  analyzeNewsText(request: AiAnalyzeNewsRequest): Observable<AiAnalyzeNewsResponse> {
    return this.http.post<AiAnalyzeNewsResponse>(`${API_BASE_URL}/ai/analyze-news-text`, request);
  }
}
