import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { GraphResponse } from '../models/graph.model';

@Injectable({ providedIn: 'root' })
export class GraphService {
  private http = inject(HttpClient);

  getNewsGraph(id: string): Observable<GraphResponse> {
    return this.http.get<GraphResponse>(`${API_BASE_URL}/graph/news/${id}`);
  }
}
