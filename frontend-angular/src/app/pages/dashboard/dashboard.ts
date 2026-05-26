import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import { NewsService } from '../../core/services/news.service';
import { DashboardSummary } from '../../core/models/dashboard.model';
import { NewsSummary } from '../../core/models/news.model';
import { DonutChart, DonutItem } from '../../components/donut-chart/donut-chart';

@Component({
  selector: 'nv-dashboard',
  imports: [RouterLink, DonutChart],
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

  /** Segmentos de la dona de riesgo (mantiene los colores de riesgo existentes). */
  riskSegments = computed<DonutItem[]>(() => {
    const d = this.data();
    if (!d) return [];
    return [
      { label: 'Bajo riesgo', value: d.lowRiskNews, color: 'var(--color-low)' },
      { label: 'Riesgo medio', value: d.mediumRiskNews, color: 'var(--color-medium)' },
      { label: 'Alto riesgo', value: d.highRiskNews, color: 'var(--color-high)' }
    ];
  });

  /** Segmentos de la dona de confiabilidad de fuentes. */
  sourceSegments = computed<DonutItem[]>(() => {
    const d = this.data();
    if (!d) return [];
    return [
      { label: 'Confiabilidad alta', value: d.highCredibilitySources, color: 'var(--color-low)' },
      { label: 'Confiabilidad media', value: d.mediumCredibilitySources, color: 'var(--color-medium)' },
      { label: 'Confiabilidad baja', value: d.lowCredibilitySources, color: 'var(--color-high)' }
    ];
  });

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
