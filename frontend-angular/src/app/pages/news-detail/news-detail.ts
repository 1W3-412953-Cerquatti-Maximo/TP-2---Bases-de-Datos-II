import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { NewsService } from '../../core/services/news.service';
import { GraphService } from '../../core/services/graph.service';
import { AiService } from '../../core/services/ai.service';
import { NewsDetail as NewsDetailModel, RiskLevel } from '../../core/models/news.model';
import { NewsAnalysis } from '../../core/models/analysis.model';
import { AiAnalyzeNewsResponse } from '../../core/models/ai.model';
import { GraphResponse } from '../../core/models/graph.model';
import { GraphViewer } from '../../components/graph-viewer/graph-viewer';

@Component({
  selector: 'nv-news-detail',
  imports: [DatePipe, RouterLink, GraphViewer],
  templateUrl: './news-detail.html',
  styleUrl: './news-detail.scss'
})
export class NewsDetail {
  private newsService = inject(NewsService);
  private graphService = inject(GraphService);
  private aiService = inject(AiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  detail = signal<NewsDetailModel | null>(null);
  graph = signal<GraphResponse | null>(null);
  analysis = signal<NewsAnalysis | null>(null);

  loading = signal(true);
  error = signal<string | null>(null);

  analysisLoading = signal(false);
  analysisError = signal<string | null>(null);

  // Asistente IA (opcional, no afecta el riskScore determinístico)
  aiResult = signal<AiAnalyzeNewsResponse | null>(null);
  aiLoading = signal(false);
  aiError = signal<string | null>(null);

  // Estado abierto/cerrado de los paneles de resultado (acordeones colapsables).
  analysisOpen = signal(false);
  aiOpen = signal(false);

  // Eliminación de noticia.
  showDeleteConfirm = signal(false);
  deleting = signal(false);
  deleteError = signal<string | null>(null);

  constructor() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) this.load(id);
    });
  }

  private load(id: string) {
    this.loading.set(true);
    this.error.set(null);
    this.detail.set(null);
    this.graph.set(null);
    this.analysis.set(null);
    this.analysisError.set(null);
    this.aiResult.set(null);
    this.aiError.set(null);
    this.analysisOpen.set(false);
    this.aiOpen.set(false);

    forkJoin({
      detail: this.newsService.getById(id),
      graph: this.graphService.getNewsGraph(id)
    }).subscribe({
      next: ({ detail, graph }) => {
        this.detail.set(detail);
        this.graph.set(graph);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 404) {
          this.error.set('La noticia no existe.');
        } else {
          this.error.set(err?.message ?? 'No se pudo cargar la noticia');
        }
        this.loading.set(false);
      }
    });
  }

  runAnalysis() {
    const d = this.detail();
    if (!d || this.analysisLoading()) return;

    this.analysisLoading.set(true);
    this.analysisError.set(null);

    this.newsService.analyze(d.id).subscribe({
      next: (a) => {
        this.analysis.set(a);
        this.analysisOpen.set(true); // abrir el acordeón al obtener resultado
        this.analysisLoading.set(false);
        // refrescamos el header con el score recién calculado
        this.detail.update(curr => curr
          ? { ...curr, riskScore: a.riskScore, riskLevel: a.riskLevel }
          : curr);
      },
      error: (err) => {
        this.analysisError.set(err?.message ?? 'No se pudo calcular el análisis');
        this.analysisLoading.set(false);
      }
    });
  }

  runAiAnalysis() {
    const d = this.detail();
    if (!d || this.aiLoading()) return;

    this.aiLoading.set(true);
    this.aiError.set(null);

    this.aiService.analyzeNewsText({
      title: d.title,
      content: d.content,
      url: d.url,
      sourceName: d.source?.name ?? null,
      topicNames: d.topics.map(t => t.name),
      riskScore: d.riskScore,
      riskLevel: d.riskLevel
    }).subscribe({
      next: (res) => {
        this.aiResult.set(res);
        this.aiOpen.set(true); // abrir el acordeón al obtener resultado
        this.aiLoading.set(false);
      },
      error: (err) => {
        // El fallo de IA no bloquea el flujo principal.
        if (err?.status === 0) {
          this.aiError.set('No se pudo conectar con el backend para el asistente IA.');
        } else {
          this.aiError.set(err?.message ?? 'El asistente IA no está disponible.');
        }
        this.aiLoading.set(false);
      }
    });
  }

  toggleAnalysisPanel(): void {
    this.analysisOpen.update(open => !open);
  }

  toggleAiPanel(): void {
    this.aiOpen.update(open => !open);
  }

  requestDelete(): void {
    this.deleteError.set(null);
    this.showDeleteConfirm.set(true);
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
  }

  confirmDelete(): void {
    const d = this.detail();
    if (!d || this.deleting()) return;

    this.deleting.set(true);
    this.deleteError.set(null);

    this.newsService.delete(d.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.showDeleteConfirm.set(false);
        this.router.navigate(['/news']);
      },
      error: (err) => {
        if (err?.status === 404) {
          this.deleteError.set('La noticia no existe o no te pertenece.');
        } else if (err?.status === 0) {
          this.deleteError.set('No se pudo conectar con el backend.');
        } else {
          this.deleteError.set(err?.error?.message ?? err?.message ?? 'No se pudo eliminar la noticia.');
        }
        this.deleting.set(false);
      }
    });
  }

  riskBadgeClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    if (level === 'LOW') return 'badge badge-low';
    return 'badge';
  }

  /** Badge para el nivel de riesgo estimado por la IA (string, no RiskLevel oficial). */
  aiRiskBadgeClass(level: string | null | undefined): string {
    if (level === 'HIGH') return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    if (level === 'LOW') return 'badge badge-low';
    return 'badge';
  }

  aiConfidencePct(confidence: number | null | undefined): number {
    return Math.round((confidence ?? 0) * 100);
  }

  scoreBarClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'score-bar high';
    if (level === 'MEDIUM') return 'score-bar medium';
    if (level === 'LOW') return 'score-bar low';
    return 'score-bar';
  }

}
