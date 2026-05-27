import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SourceService } from '../../core/services/source.service';
import { Source } from '../../core/models/source.model';
import { SelectMenu, SelectOption } from '../../components/select-menu/select-menu';

type SortKey = 'name-asc' | 'name-desc' | 'cred-asc' | 'cred-desc';
// Niveles de confiabilidad (HIGH = Alta, MEDIUM = Media, LOW = Baja).
type ReliabilityKey = '' | 'HIGH' | 'MEDIUM' | 'LOW';

const DEFAULT_SORT: SortKey = 'cred-desc';

@Component({
  selector: 'nv-sources',
  imports: [FormsModule, SelectMenu],
  templateUrl: './sources.html',
  styleUrl: './sources.scss'
})
export class Sources {
  private service = inject(SourceService);

  sources = signal<Source[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  // Búsqueda / filtros / orden: todo local sobre la lista ya cargada (sin llamadas extra).
  searchTerm = signal('');
  selectedType = signal('');
  selectedReliability = signal<ReliabilityKey>('');
  selectedSort = signal<SortKey>(DEFAULT_SORT);

  totalSources = computed(() => this.sources().length);

  /** Opciones de orden para el dropdown custom. */
  readonly sortOptions: SelectOption[] = [
    { value: 'name-asc', label: 'Nombre A-Z' },
    { value: 'name-desc', label: 'Nombre Z-A' },
    { value: 'cred-asc', label: 'Confiabilidad menor a mayor' },
    { value: 'cred-desc', label: 'Confiabilidad mayor a menor' }
  ];

  /** Tipos presentes en las fuentes, ordenados A-Z (generados dinámicamente). */
  availableTypes = computed(() => {
    const set = new Set<string>();
    for (const s of this.sources()) {
      if (s.type) set.add(s.type);
    }
    return [...set].sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
  });

  /** Opciones para el dropdown de Tipo (incluye "Todos los tipos"). */
  typeOptions = computed<SelectOption[]>(() => [
    { value: '', label: 'Todos los tipos' },
    ...this.availableTypes().map(t => ({ value: t, label: t }))
  ]);

  hasActiveFilters = computed(() =>
    this.searchTerm().trim() !== '' ||
    this.selectedType() !== '' ||
    this.selectedReliability() !== '' ||
    this.selectedSort() !== DEFAULT_SORT
  );

  /** Lista filtrada + ordenada (los filtros se aplican antes del orden). */
  filteredSources = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.selectedType();
    const reliability = this.selectedReliability();

    const filtered = this.sources().filter(s => {
      if (term && !(s.name ?? '').toLowerCase().includes(term)) return false;
      if (type && s.type !== type) return false;
      if (reliability && this.reliabilityOf(s.credibilityScore) !== reliability) return false;
      return true;
    });

    return this.sortSources(filtered, this.selectedSort());
  });

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

  setReliability(level: ReliabilityKey): void {
    this.selectedReliability.set(level);
  }

  setSort(value: string): void {
    this.selectedSort.set(value as SortKey);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedType.set('');
    this.selectedReliability.set('');
    this.selectedSort.set(DEFAULT_SORT);
  }

  /** Bucket de confiabilidad. credibilityScore null se trata como 0 → Baja (igual que en el orden). */
  private reliabilityOf(score: number | null): ReliabilityKey {
    const s = score ?? 0;
    if (s >= 0.7) return 'HIGH';
    if (s >= 0.4) return 'MEDIUM';
    return 'LOW';
  }

  private sortSources(list: Source[], key: SortKey): Source[] {
    const arr = [...list];
    // name null → '' ; credibilityScore null → 0.
    const byStr = (a: string | null, b: string | null, dir: 1 | -1) =>
      dir * (a ?? '').localeCompare(b ?? '', 'es', { sensitivity: 'base' });
    const byNum = (a: number | null, b: number | null, dir: 1 | -1) =>
      dir * ((a ?? 0) - (b ?? 0));

    switch (key) {
      case 'name-asc':  arr.sort((a, b) => byStr(a.name, b.name, 1));  break;
      case 'name-desc': arr.sort((a, b) => byStr(a.name, b.name, -1)); break;
      case 'cred-asc':  arr.sort((a, b) => byNum(a.credibilityScore, b.credibilityScore, 1));  break;
      case 'cred-desc': arr.sort((a, b) => byNum(a.credibilityScore, b.credibilityScore, -1)); break;
    }
    return arr;
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
