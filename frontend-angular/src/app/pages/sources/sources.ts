import { Component, inject, signal } from '@angular/core';

import { SourceService } from '../../core/services/source.service';
import { Source } from '../../core/models/source.model';

@Component({
  selector: 'nv-sources',
  imports: [],
  templateUrl: './sources.html',
  styleUrl: './sources.scss'
})
export class Sources {
  private service = inject(SourceService);

  sources = signal<Source[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor() {
    this.service.list().subscribe({
      next: (data) => {
        this.sources.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'No se pudo obtener las fuentes');
        this.loading.set(false);
      }
    });
  }

  credibilityBarClass(score: number | null): string {
    if (score === null) return 'score-bar';
    if (score >= 0.7) return 'score-bar low';        // verde = alta confiabilidad
    if (score >= 0.4) return 'score-bar medium';
    return 'score-bar high';                          // rojo = baja confiabilidad
  }

  credibilityLabel(score: number | null): string {
    if (score === null) return 'N/A';
    if (score >= 0.7) return 'Alta';
    if (score >= 0.4) return 'Media';
    return 'Baja';
  }

  credibilityBadgeClass(score: number | null): string {
    if (score === null) return 'badge';
    if (score >= 0.7) return 'badge badge-low';
    if (score >= 0.4) return 'badge badge-medium';
    return 'badge badge-high';
  }

  percent(score: number | null): number {
    if (score === null) return 0;
    return Math.round(score * 100);
  }
}
