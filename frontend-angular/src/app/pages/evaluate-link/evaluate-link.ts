import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { NewsService } from '../../core/services/news.service';
import { EvaluateLinkResponse } from '../../core/models/link-evaluation.model';
import { SubmitNewsUrlRequest, SubmitNewsUrlResponse } from '../../core/models/submit-news.model';
import { RiskLevel } from '../../core/models/news.model';

@Component({
  selector: 'nv-evaluate-link',
  imports: [FormsModule, RouterLink],
  templateUrl: './evaluate-link.html',
  styleUrl: './evaluate-link.scss'
})
export class EvaluateLink {
  private newsService = inject(NewsService);

  url = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<EvaluateLinkResponse | null>(null);

  // Guardado de la noticia por URL.
  saving = signal(false);
  saveError = signal<string | null>(null);
  saveResult = signal<SubmitNewsUrlResponse | null>(null);

  diagnosis = computed(() => this.result()?.credibilityDiagnosis ?? null);
  aiResult = computed(() => this.result()?.aiAnalysis ?? null);

  /** Habilita "Guardar noticia" solo con una URL http/https con formato razonable. */
  canSave(): boolean {
    return !this.saving() && /^https?:\/\/.+/i.test(this.url.trim());
  }

  submit(): void {
    const trimmedUrl = this.url.trim();
    if (!trimmedUrl) {
      this.error.set('Ingresá un link para evaluar.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.newsService.evaluateLink({ url: trimmedUrl }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend.');
        } else {
          this.error.set(err?.error?.message ?? err?.message ?? 'No se pudo evaluar el enlace.');
        }
        this.loading.set(false);
      }
    });
  }

  save(): void {
    const trimmedUrl = this.url.trim();
    if (!this.canSave()) {
      this.saveError.set('Ingresá una URL válida (http o https) para guardar la noticia.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    this.saveResult.set(null);

    // Reutilizamos lo ya evaluado si está disponible; si no, mandamos solo la URL.
    const evaluation = this.result();
    const diagnosis = this.diagnosis();
    const ai = this.aiResult();
    const request: SubmitNewsUrlRequest = { url: trimmedUrl };
    if (evaluation?.title) request.title = evaluation.title;
    if (evaluation?.contentPreview) request.content = evaluation.contentPreview;
    if (ai?.enabled && ai.suggestedTopics.length > 0) request.topicNames = ai.suggestedTopics;
    // Persistimos el riesgo evaluado para que /news muestre el mismo score.
    if (diagnosis) {
      request.riskScore = diagnosis.riskScore;
      request.riskLevel = diagnosis.riskLevel;
    }

    this.newsService.submitNewsUrl(request).subscribe({
      next: (response) => {
        this.saveResult.set(response);
        this.saving.set(false);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.saveError.set('No se pudo conectar con el backend.');
        } else {
          this.saveError.set(err?.error?.message ?? err?.message ?? 'No se pudo guardar la noticia.');
        }
        this.saving.set(false);
      }
    });
  }

  saveHasWarnings(): boolean {
    const ext = this.saveResult()?.extraction;
    return !!ext && (!ext.success || ext.warnings.length > 0);
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

  fetchStatusClass(fetchStatus: string | null): string {
    return fetchStatus === 'OK' ? 'badge badge-low' : 'badge badge-medium';
  }
}
