import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { Source } from '../models/source.model';

@Injectable({ providedIn: 'root' })
export class SourceService {
  private http = inject(HttpClient);

  list(): Observable<Source[]> {
    return this.http.get<Source[]>(`${API_BASE_URL}/sources`);
  }
}
