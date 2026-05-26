import { Component, computed, inject, signal } from '@angular/core';
import { catchError, of } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import {
  DashboardSummary,
  GraphSummaryNode,
  GraphSummaryResponse,
  NewsTimelineItem,
  RiskSignalSummaryItem,
  TopicRiskRankingItem
} from '../../core/models/dashboard.model';
import { DonutChart, DonutItem } from '../../components/donut-chart/donut-chart';

interface TimelineBar {
  label: string;
  total: number;
  lowPct: number;
  mediumPct: number;
  highPct: number;
  low: number;
  medium: number;
  high: number;
}

interface GraphPlacedNode extends GraphSummaryNode {
  x: number;
  y: number;
  color: string;
}

const GRAPH_COLORS: Record<string, string> = {
  News: '#38BDF8',
  Source: '#14B8A6',
  Topic: '#A78BFA',
  Claim: '#F59E0B',
  Evidence: '#22C55E',
  FactCheck: '#2DD4BF',
  Post: '#F472B6',
  User: '#94A3B8'
};

@Component({
  selector: 'nv-dashboard',
  imports: [DonutChart],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  private dashboardService = inject(DashboardService);

  data = signal<DashboardSummary | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  // Visualizaciones analíticas (cada una carga de forma independiente y resiliente).
  topicRanking = signal<TopicRiskRankingItem[]>([]);
  riskSignals = signal<RiskSignalSummaryItem[]>([]);
  timeline = signal<NewsTimelineItem[]>([]);
  graph = signal<GraphSummaryResponse | null>(null);

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

  // --- Evolución temporal: barras apiladas ---
  timelineBars = computed<TimelineBar[]>(() => {
    const items = this.timeline();
    const maxTotal = items.reduce((max, d) => Math.max(max, d.total), 0);
    return items.map(d => ({
      label: this.formatDay(d.date),
      total: d.total,
      low: d.low,
      medium: d.medium,
      high: d.high,
      // altura de cada segmento como % del día más cargado
      lowPct: maxTotal === 0 ? 0 : (d.low / maxTotal) * 100,
      mediumPct: maxTotal === 0 ? 0 : (d.medium / maxTotal) * 100,
      highPct: maxTotal === 0 ? 0 : (d.high / maxTotal) * 100
    }));
  });

  // --- Mini grafo: layout radial fijo (News al centro, resto alrededor) ---
  readonly graphView = { w: 460, h: 300, cx: 230, cy: 150, r: 112 };

  graphCenter = computed<GraphPlacedNode | null>(() => {
    const g = this.graph();
    if (!g) return null;
    const center = g.nodes.find(n => n.type === 'News') ?? g.nodes[0];
    if (!center) return null;
    return { ...center, x: this.graphView.cx, y: this.graphView.cy, color: this.graphColor(center.type) };
  });

  graphOuter = computed<GraphPlacedNode[]>(() => {
    const g = this.graph();
    const center = this.graphCenter();
    if (!g || !center) return [];
    const outer = g.nodes.filter(node => node.type !== center.type);
    const n = outer.length || 1;
    return outer.map((node, i) => {
      const angle = (-90 + i * (360 / n)) * (Math.PI / 180);
      return {
        ...node,
        x: this.graphView.cx + this.graphView.r * Math.cos(angle),
        y: this.graphView.cy + this.graphView.r * Math.sin(angle),
        color: this.graphColor(node.type)
      };
    });
  });

  graphHasData = computed(() => {
    const g = this.graph();
    if (!g) return false;
    return g.nodes.some(n => n.count > 0) || g.totalRelationships > 0;
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
    // Se loguea el error (no se oculta del todo) para facilitar el desarrollo.
    this.dashboardService.getTopicRiskRanking()
      .pipe(catchError(err => { console.error('[dashboard] topic-risk-ranking falló', err); return of<TopicRiskRankingItem[]>([]); }))
      .subscribe(v => this.topicRanking.set(v));

    this.dashboardService.getRiskSignals()
      .pipe(catchError(err => { console.error('[dashboard] risk-signals falló', err); return of<RiskSignalSummaryItem[]>([]); }))
      .subscribe(v => this.riskSignals.set(v));

    this.dashboardService.getNewsTimeline()
      .pipe(catchError(err => { console.error('[dashboard] news-timeline falló', err); return of<NewsTimelineItem[]>([]); }))
      .subscribe(v => this.timeline.set(v));

    this.dashboardService.getGraphSummary()
      .pipe(catchError(err => { console.error('[dashboard] graph-summary falló', err); return of<GraphSummaryResponse | null>(null); }))
      .subscribe(v => this.graph.set(v));
  }

  /** Color de la barra de tema según su riesgo promedio. */
  topicBarColor(avg: number): string {
    if (avg >= 70) return 'var(--color-high)';
    if (avg >= 40) return 'var(--color-medium)';
    return 'var(--color-low)';
  }

  graphColor(type: string): string {
    return GRAPH_COLORS[type] ?? 'var(--color-accent)';
  }

  private formatDay(iso: string): string {
    // 'YYYY-MM-DD' -> 'DD/MM'
    const parts = (iso ?? '').split('-');
    return parts.length === 3 ? `${parts[2]}/${parts[1]}` : iso;
  }
}
