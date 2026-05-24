import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { NewsService } from '../../core/services/news.service';
import { SourceService } from '../../core/services/source.service';
import { NewsSummary, RiskLevel } from '../../core/models/news.model';
import { Source } from '../../core/models/source.model';

interface RiskBucket {
  level: RiskLevel;
  count: number;
  percentage: number;
}

@Component({
  selector: 'nv-reports',
  imports: [RouterLink],
  templateUrl: './reports.html',
  styleUrl: './reports.scss'
})
export class Reports {
  private newsService = inject(NewsService);
  private sourceService = inject(SourceService);

  news = signal<NewsSummary[]>([]);
  sources = signal<Source[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  /** Distribución por riesgo a partir del listado. */
  distribution = computed<RiskBucket[]>(() => {
    const list = this.news();
    const total = list.length || 1;
    const counts: Record<RiskLevel, number> = { LOW: 0, MEDIUM: 0, HIGH: 0 };
    for (const n of list) {
      if (n.riskLevel) counts[n.riskLevel]++;
    }
    return (['LOW', 'MEDIUM', 'HIGH'] as RiskLevel[]).map(level => ({
      level,
      count: counts[level],
      percentage: Math.round((counts[level] / total) * 100)
    }));
  });

  /** Fuentes ordenadas por MENOR credibilidad (las más riesgosas primero). */
  sourcesByLowCredibility = computed(() =>
    [...this.sources()].sort((a, b) =>
      (a.credibilityScore ?? 0) - (b.credibilityScore ?? 0)
    )
  );

  /** Top 5 noticias de mayor riskScore. */
  topRiskyNews = computed(() =>
    [...this.news()]
      .sort((a, b) => (b.riskScore ?? 0) - (a.riskScore ?? 0))
      .slice(0, 5)
  );

  constructor() {
    forkJoin({
      news: this.newsService.list(),
      sources: this.sourceService.list()
    }).subscribe({
      next: ({ news, sources }) => {
        this.news.set(news);
        this.sources.set(sources);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend (¿está corriendo en http://localhost:8080?).');
        } else {
          this.error.set(err?.message ?? 'No se pudo cargar los reportes');
        }
        this.loading.set(false);
      }
    });
  }

  bucketClass(level: RiskLevel): string {
    return `bar-${level.toLowerCase()}`;
  }

  badgeClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    if (level === 'LOW') return 'badge badge-low';
    return 'badge';
  }

  credBarClass(score: number | null): string {
    if (score === null) return 'score-bar';
    if (score >= 0.7) return 'score-bar low';
    if (score >= 0.4) return 'score-bar medium';
    return 'score-bar high';
  }

  percent(score: number | null): number {
    if (score === null) return 0;
    return Math.round(score * 100);
  }
}
