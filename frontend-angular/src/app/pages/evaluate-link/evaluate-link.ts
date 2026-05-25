import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { NewsService } from '../../core/services/news.service';
import { EvaluateLinkResponse } from '../../core/models/link-evaluation.model';
import { RiskLevel } from '../../core/models/news.model';

@Component({
  selector: 'nv-evaluate-link',
  imports: [FormsModule],
  templateUrl: './evaluate-link.html',
  styleUrl: './evaluate-link.scss'
})
export class EvaluateLink {
  private newsService = inject(NewsService);

  url = '';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<EvaluateLinkResponse | null>(null);

  diagnosis = computed(() => this.result()?.credibilityDiagnosis ?? null);
  aiResult = computed(() => this.result()?.aiAnalysis ?? null);

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
