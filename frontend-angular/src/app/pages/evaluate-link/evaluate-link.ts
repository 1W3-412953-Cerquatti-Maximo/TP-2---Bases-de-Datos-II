import { Component, inject, signal } from '@angular/core';
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

  // "Evaluar y guardar": guarda la noticia y corre el pipeline oficial.
  saving = signal(false);
  saveError = signal<string | null>(null);
  saveResult = signal<SubmitNewsUrlResponse | null>(null);

  /**
   * URL (normalizada localmente) de la última noticia guardada exitosamente o
   * detectada como ya existente. Mientras el input coincida con esta URL, el
   * botón "Evaluar y guardar" queda deshabilitado para evitar reenvíos.
   */
  private lastSavedUrl = signal<string | null>(null);

  /** Habilita "Evaluar y guardar" solo con URL válida y distinta de la última guardada. */
  canSave(): boolean {
    const current = this.url.trim();
    if (this.saving()) return false;
    if (!/^https?:\/\/.+/i.test(current)) return false;
    return this.normalize(current) !== this.lastSavedUrl();
  }

  /** ¿La URL del input coincide con la última guardada? (mensaje discreto bajo el botón). */
  alreadySavedHint(): boolean {
    const current = this.url.trim();
    if (!current) return false;
    return this.normalize(current) === this.lastSavedUrl();
  }

  /**
   * Cambio en el input URL: limpia preview, resultado guardado y errores. Si la
   * URL nueva difiere de la última guardada, libera el bloqueo del botón.
   */
  onUrlChange(value: string): void {
    this.url = value;
    const normalized = this.normalize(value.trim());
    if (normalized !== this.lastSavedUrl()) {
      // Estado anterior queda obsoleto al escribir una URL distinta.
      this.result.set(null);
      this.error.set(null);
      this.saveResult.set(null);
      this.saveError.set(null);
    }
  }

  /**
   * "Buscar noticia": SOLO recupera y muestra una vista previa del contenido.
   * No calcula riskScore, no muestra señales de diagnóstico, no ejecuta IA, no guarda.
   * Limpia la tarjeta verde anterior antes de mostrar la preview de la URL actual.
   */
  submit(): void {
    const trimmedUrl = this.url.trim();
    if (!trimmedUrl) {
      this.error.set('Ingresá un link para evaluar.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    // Tarjeta de noticia guardada anterior no debe coexistir con una preview nueva.
    this.saveResult.set(null);
    this.saveError.set(null);

    this.newsService.evaluateLink({ url: trimmedUrl }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend.');
        } else {
          this.error.set(err?.error?.message ?? err?.message ?? 'No se pudo recuperar el enlace.');
        }
        this.loading.set(false);
      }
    });
  }

  /**
   * "Evaluar y guardar": guarda la noticia y corre el pipeline oficial
   * (riskScore + enriquecimiento IA). El backend re-extrae el cuerpo completo;
   * mandamos el contenido extraído como fallback por si la re-extracción falla.
   * Si el backend detecta que ya existe (alreadyExists=true), no duplica y
   * la respuesta trae los datos de la noticia existente.
   */
  save(): void {
    const trimmedUrl = this.url.trim();
    if (!this.canSave()) {
      this.saveError.set('Ingresá una URL válida (http o https) para guardar la noticia.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    this.saveResult.set(null);

    const evaluation = this.result();
    const request: SubmitNewsUrlRequest = { url: trimmedUrl };
    if (evaluation?.title) request.title = evaluation.title;
    const fullContent = evaluation?.content || evaluation?.contentPreview;
    if (fullContent) request.content = fullContent;

    this.newsService.submitNewsUrl(request).subscribe({
      next: (response) => {
        this.saveResult.set(response);
        // Tanto en guardado nuevo como en alreadyExists, bloqueamos el botón
        // hasta que el usuario escriba una URL distinta.
        this.lastSavedUrl.set(this.normalize(trimmedUrl));
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
    const saved = this.saveResult();
    if (!saved || saved.alreadyExists) return false; // dedup no trae extraction real
    const ext = saved.extraction;
    return !!ext && (!ext.success || ext.warnings.length > 0);
  }

  /** Badge LOW/MEDIUM/HIGH — usado por la tarjeta verde de noticia guardada. */
  riskBadgeClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'badge badge-high';
    if (level === 'MEDIUM') return 'badge badge-medium';
    if (level === 'LOW') return 'badge badge-low';
    return 'badge';
  }

  /** Host de la URL para mostrar como "Fuente" en la vista previa. */
  hostFromUrl(url: string): string {
    try { return new URL(url).host; } catch { return ''; }
  }

  /**
   * Normalización local equivalente a la del backend: scheme/host a minúsculas,
   * sin slash final, sin fragmento. Usada para comparar la URL del input con la
   * última guardada y deshabilitar el botón en consecuencia.
   */
  private normalize(raw: string): string {
    const trimmed = (raw ?? '').trim();
    if (!trimmed) return '';
    try {
      const u = new URL(trimmed);
      let path = u.pathname || '';
      if (path.length > 1 && path.endsWith('/')) path = path.slice(0, -1);
      const port = u.port ? `:${u.port}` : '';
      const query = u.search ?? '';
      return `${u.protocol}//${u.host.toLowerCase()}${port}${path}${query}`;
    } catch {
      return trimmed;
    }
  }
}
