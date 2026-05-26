import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { NewsService } from '../../core/services/news.service';
import { NewsSummary, RiskLevel } from '../../core/models/news.model';
import { SelectMenu, SelectOption } from '../../components/select-menu/select-menu';

type SortKey =
  | 'date-desc' | 'date-asc'
  | 'title-asc' | 'title-desc'
  | 'source-asc' | 'source-desc'
  | 'score-asc' | 'score-desc';

const DEFAULT_SORT: SortKey = 'date-desc';

@Component({
  selector: 'nv-news-list',
  imports: [DatePipe, RouterLink, FormsModule, SelectMenu],
  templateUrl: './news-list.html',
  styleUrl: './news-list.scss'
})
export class NewsList {
  private service = inject(NewsService);
  private router = inject(Router);

  news = signal<NewsSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  // Búsqueda / filtros / orden (todo local sobre la lista ya cargada).
  searchTerm = signal('');
  selectedSource = signal('');
  selectedTopic = signal('');
  selectedRiskLevel = signal<RiskLevel | ''>('');
  selectedSort = signal<SortKey>(DEFAULT_SORT);

  // Eliminación de noticia.
  pendingDelete = signal<NewsSummary | null>(null);
  deleting = signal(false);
  deleteError = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  totalNews = computed(() => this.news().length);

  /** Opciones de orden para el dropdown custom. */
  readonly sortOptions: SelectOption[] = [
    { value: 'date-desc', label: 'Fecha más reciente' },
    { value: 'date-asc', label: 'Fecha más antigua' },
    { value: 'title-asc', label: 'Título A-Z' },
    { value: 'title-desc', label: 'Título Z-A' },
    { value: 'source-asc', label: 'Fuente A-Z' },
    { value: 'source-desc', label: 'Fuente Z-A' },
    { value: 'score-asc', label: 'Score menor a mayor' },
    { value: 'score-desc', label: 'Score mayor a menor' }
  ];

  /** Fuentes presentes en las noticias, ordenadas A-Z (sin IDs). */
  availableSources = computed(() => {
    const set = new Set<string>();
    for (const n of this.news()) {
      if (n.sourceName) set.add(n.sourceName);
    }
    return [...set].sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
  });

  /** Temas presentes (a partir de topicNames), ordenados A-Z. */
  availableTopics = computed(() => {
    const set = new Set<string>();
    for (const n of this.news()) {
      for (const t of n.topicNames ?? []) {
        if (t) set.add(t);
      }
    }
    return [...set].sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
  });

  /** Opciones para el dropdown de Fuente (incluye "Todas las fuentes"). */
  sourceOptions = computed<SelectOption[]>(() => [
    { value: '', label: 'Todas las fuentes' },
    ...this.availableSources().map(s => ({ value: s, label: s }))
  ]);

  /** Opciones para el dropdown de Tema (incluye "Todos los temas"). */
  topicOptions = computed<SelectOption[]>(() => [
    { value: '', label: 'Todos los temas' },
    ...this.availableTopics().map(t => ({ value: t, label: t }))
  ]);

  hasActiveFilters = computed(() =>
    this.searchTerm().trim() !== '' ||
    this.selectedSource() !== '' ||
    this.selectedTopic() !== '' ||
    this.selectedRiskLevel() !== '' ||
    this.selectedSort() !== DEFAULT_SORT
  );

  /** Lista filtrada + ordenada (los filtros se aplican antes del orden). */
  filteredNews = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const source = this.selectedSource();
    const topic = this.selectedTopic();
    const risk = this.selectedRiskLevel();

    const filtered = this.news().filter(n => {
      if (term && !(n.title ?? '').toLowerCase().includes(term)) return false;
      if (source && n.sourceName !== source) return false;
      if (topic && !(n.topicNames ?? []).includes(topic)) return false;
      if (risk && n.riskLevel !== risk) return false;
      return true;
    });

    return this.sortNews(filtered, this.selectedSort());
  });

  constructor() {
    this.service.list().subscribe({
      next: (data) => {
        this.news.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'No se pudo obtener el listado');
        this.loading.set(false);
      }
    });
  }

  setRisk(level: RiskLevel | ''): void {
    this.selectedRiskLevel.set(level);
  }

  setSort(value: string): void {
    this.selectedSort.set(value as SortKey);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedSource.set('');
    this.selectedTopic.set('');
    this.selectedRiskLevel.set('');
    this.selectedSort.set(DEFAULT_SORT);
  }

  private sortNews(list: NewsSummary[], key: SortKey): NewsSummary[] {
    const arr = [...list];
    const byStr = (a: string | null, b: string | null, dir: 1 | -1) =>
      dir * (a ?? '').localeCompare(b ?? '', 'es', { sensitivity: 'base' });
    const byNum = (a: number | null, b: number | null, dir: 1 | -1) =>
      dir * ((a ?? 0) - (b ?? 0));
    // publishedAt null siempre al final, sin importar la dirección.
    const byDate = (a: string | null, b: string | null, dir: 1 | -1) => {
      const ta = a ? Date.parse(a) : null;
      const tb = b ? Date.parse(b) : null;
      if (ta === null && tb === null) return 0;
      if (ta === null) return 1;
      if (tb === null) return -1;
      return dir * (ta - tb);
    };

    switch (key) {
      case 'date-desc':   arr.sort((a, b) => byDate(a.publishedAt, b.publishedAt, -1)); break;
      case 'date-asc':    arr.sort((a, b) => byDate(a.publishedAt, b.publishedAt, 1));  break;
      case 'title-asc':   arr.sort((a, b) => byStr(a.title, b.title, 1));   break;
      case 'title-desc':  arr.sort((a, b) => byStr(a.title, b.title, -1));  break;
      case 'source-asc':  arr.sort((a, b) => byStr(a.sourceName, b.sourceName, 1));  break;
      case 'source-desc': arr.sort((a, b) => byStr(a.sourceName, b.sourceName, -1)); break;
      case 'score-asc':   arr.sort((a, b) => byNum(a.riskScore, b.riskScore, 1));  break;
      case 'score-desc':  arr.sort((a, b) => byNum(a.riskScore, b.riskScore, -1)); break;
    }
    return arr;
  }

  /** Abre la confirmación sin navegar al detalle (la fila es clickable). */
  requestDelete(news: NewsSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.deleteError.set(null);
    this.pendingDelete.set(news);
  }

  cancelDelete(): void {
    this.pendingDelete.set(null);
  }

  confirmDelete(): void {
    const target = this.pendingDelete();
    if (!target || this.deleting()) return;

    this.deleting.set(true);
    this.deleteError.set(null);

    this.service.delete(target.id).subscribe({
      next: (res) => {
        // Removemos del listado en memoria, sin volver a pedir /api/news.
        this.news.update(list => list.filter(n => n.id !== target.id));
        let message = `Noticia "${target.title}" eliminada.`;
        if (res.sourceDeleted) {
          message += ' También se eliminó la fuente porque no tenía más noticias asociadas.';
        }
        this.successMessage.set(message);
        this.deleting.set(false);
        this.pendingDelete.set(null);
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

  scoreBarClass(level: RiskLevel | null): string {
    if (level === 'HIGH') return 'score-bar high';
    if (level === 'MEDIUM') return 'score-bar medium';
    if (level === 'LOW') return 'score-bar low';
    return 'score-bar';
  }

  goTo(id: string) {
    this.router.navigate(['/news', id]);
  }
}
