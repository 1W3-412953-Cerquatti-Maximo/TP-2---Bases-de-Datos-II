import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { NewsService } from '../../core/services/news.service';
import { NewsSummary, RiskLevel } from '../../core/models/news.model';

@Component({
  selector: 'nv-news-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './news-list.html',
  styleUrl: './news-list.scss'
})
export class NewsList {
  private service = inject(NewsService);
  private router = inject(Router);

  news = signal<NewsSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor() {
    this.service.list().subscribe({
      next: (data) => {
        this.news.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'No se pudo obtener el listado');
        this.loading.set(false);
      }
    });
  }

  riskBadgeClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    if (level === 'LOW') return 'badge badge-low';
    return 'badge';
  }

  scoreBarClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'score-bar high';
    if (level === 'MEDIUM') return 'score-bar medium';
    if (level === 'LOW') return 'score-bar low';
    return 'score-bar';
  }

  goTo(id: string) {
    this.router.navigate(['/news', id]);
  }
}
