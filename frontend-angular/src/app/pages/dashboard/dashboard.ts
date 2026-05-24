import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import { NewsService } from '../../core/services/news.service';
import { DashboardSummary } from '../../core/models/dashboard.model';
import { NewsSummary } from '../../core/models/news.model';

@Component({
  selector: 'nv-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  private dashboardService = inject(DashboardService);
  private newsService = inject(NewsService);

  data = signal<DashboardSummary | null>(null);
  news = signal<NewsSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  /** Top 5 noticias HIGH para acceso rápido. */
  highRiskShortcuts = computed(() =>
    this.news()
      .filter(n => n.riskLevel === 'HIGH')
      .sort((a, b) => (b.riskScore ?? 0) - (a.riskScore ?? 0))
      .slice(0, 5)
  );

  constructor() {
    forkJoin({
      summary: this.dashboardService.getSummary(),
      news: this.newsService.list()
    }).subscribe({
      next: ({ summary, news }) => {
        this.data.set(summary);
        this.news.set(news);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend (¿está corriendo en http://localhost:8080?).');
        } else {
          this.error.set(err?.message ?? 'Error al cargar el dashboard');
        }
        this.loading.set(false);
      }
    });
  }
}
