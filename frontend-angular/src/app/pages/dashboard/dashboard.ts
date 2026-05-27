import { Component, computed, inject, signal } from '@angular/core';
import { catchError, of } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import { NewsService } from '../../core/services/news.service';
import {
  DashboardSummary,
  GraphSummaryResponse,
  RiskSignalSummaryItem,
  TopicRiskRankingItem
} from '../../core/models/dashboard.model';
import { NewsSummary } from '../../core/models/news.model';
import { ChartDoughnut, DonutItem } from '../../components/chart-doughnut/chart-doughnut';
import { ChartStackedBar, TopicRiskDatum } from '../../components/chart-stacked-bar/chart-stacked-bar';
import { GraphOverview } from '../../components/graph-overview/graph-overview';

const TOP_TOPICS = 8;

@Component({
  selector: 'nv-dashboard',
  imports: [ChartDoughnut, ChartStackedBar, GraphOverview],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  private dashboardService = inject(DashboardService);
  private newsService = inject(NewsService);

  data = signal<DashboardSummary | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  // Visualizaciones analíticas (cada una carga de forma independiente y resiliente).
  topicRanking = signal<TopicRiskRankingItem[]>([]);
  riskSignals = signal<RiskSignalSummaryItem[]>([]);
  graph = signal<GraphSummaryResponse | null>(null);

  // Noticias crudas: alimentan el stacked bar "noticias por tema y riesgo" (cálculo en frontend).
  news = signal<NewsSummary[]>([]);

  // --- Donas (riesgo y confiabilidad) ---
  riskSegments = computed<DonutItem[]>(() => {
    const d = this.data();
    if (!d) return [];
    return [
      { label: 'Bajo riesgo', value: d.lowRiskNews, color: 'var(--color-low)' },
      { label: 'Riesgo medio', value: d.mediumRiskNews, color: 'var(--color-medium)' },
      { label: 'Alto riesgo', value: d.highRiskNews, color: 'var(--color-high)' }
    ];
  });

  sourceSegments = computed<DonutItem[]>(() => {
    const d = this.data();
    if (!d) return [];
    return [
      { label: 'Confiabilidad alta', value: d.highCredibilitySources, color: 'var(--color-low)' },
      { label: 'Confiabilidad media', value: d.mediumCredibilitySources, color: 'var(--color-medium)' },
      { label: 'Confiabilidad baja', value: d.lowCredibilitySources, color: 'var(--color-high)' }
    ];
  });

  // --- Señales de riesgo: escala de barras relativa al máximo ---
  maxSignalCount = computed(() =>
    this.riskSignals().reduce((max, s) => Math.max(max, s.count), 0)
  );

  /**
   * Cantidad de noticias por tema y riesgo (Top 8 temas por total).
   * Cada noticia suma 1 en su nivel (LOW/MEDIUM/HIGH) por cada tema asociado;
   * sin temas → "Sin tema". Las noticias sin riskLevel no se cuentan en ningún nivel.
   */
  topicRiskDistribution = computed<TopicRiskDatum[]>(() => {
    const buckets = new Map<string, { low: number; medium: number; high: number }>();

    for (const n of this.news()) {
      const topics = n.topicNames && n.topicNames.length > 0 ? n.topicNames : ['Sin tema'];
      for (const topic of topics) {
        const b = buckets.get(topic) ?? { low: 0, medium: 0, high: 0 };
        if (n.riskLevel === 'HIGH') b.high++;
        else if (n.riskLevel === 'MEDIUM') b.medium++;
        else if (n.riskLevel === 'LOW') b.low++;
        buckets.set(topic, b);
      }
    }

    return [...buckets.entries()]
      .map(([topic, b]) => ({ topic, ...b, total: b.low + b.medium + b.high }))
      .filter(d => d.total > 0)
      .sort((a, b) => b.total - a.total)
      .slice(0, TOP_TOPICS)
      .map(({ topic, low, medium, high }) => ({ topic, low, medium, high }));
  });

  constructor() {
    // Núcleo del dashboard (donas): controla loading/error general.
    this.dashboardService.getSummary().subscribe({
      next: (summary) => {
        this.data.set(summary);
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

    // Visualizaciones extra: si una falla, no rompe el resto del dashboard.
    this.dashboardService.getTopicRiskRanking()
      .pipe(catchError(err => { console.error('[dashboard] topic-risk-ranking falló', err); return of<TopicRiskRankingItem[]>([]); }))
      .subscribe(v => this.topicRanking.set(v));

    this.dashboardService.getRiskSignals()
      .pipe(catchError(err => { console.error('[dashboard] risk-signals falló', err); return of<RiskSignalSummaryItem[]>([]); }))
      .subscribe(v => this.riskSignals.set(v));

    this.dashboardService.getGraphSummary()
      .pipe(catchError(err => { console.error('[dashboard] graph-summary falló', err); return of<GraphSummaryResponse | null>(null); }))
      .subscribe(v => this.graph.set(v));

    // Noticias para el stacked bar (tema × riesgo). Si falla, queda en estado vacío.
    this.newsService.list()
      .pipe(catchError(err => { console.error('[dashboard] news (tema×riesgo) falló', err); return of<NewsSummary[]>([]); }))
      .subscribe(v => this.news.set(v));
  }

  /** Color de la barra de tema según su riesgo promedio. */
  topicBarColor(avg: number): string {
    if (avg >= 70) return 'var(--color-high)';
    if (avg >= 40) return 'var(--color-medium)';
    return 'var(--color-low)';
  }
}
