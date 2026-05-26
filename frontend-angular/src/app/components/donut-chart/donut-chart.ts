import { Component, afterNextRender, computed, input, signal } from '@angular/core';

export interface DonutItem {
  label: string;
  value: number;
  color: string;
}

interface DonutSeg {
  label: string;
  value: number;
  color: string;
  pct: number;
  isZero: boolean;
  dash: string;
  rotation: number;
}

/**
 * Dona SVG reutilizable, sin librerías externas.
 * Renderiza segmentos con separación (gaps), animación de aparición,
 * hover sincronizado con la leyenda y tooltip.
 */
@Component({
  selector: 'nv-donut-chart',
  imports: [],
  templateUrl: './donut-chart.html',
  styleUrl: './donut-chart.scss'
})
export class DonutChart {
  title = input<string>('');
  centerLabel = input<string>('');
  tooltipUnit = input<string>('');
  items = input<DonutItem[]>([]);
  emptyMessage = input<string>('Sin datos para mostrar');

  // Geometría del SVG (viewBox 0..120).
  readonly cx = 60;
  readonly cy = 60;
  readonly radius = 42;
  readonly strokeWidth = 20;
  readonly circumference = 2 * Math.PI * this.radius;

  mounted = signal(false);
  hoverIndex = signal<number | null>(null);
  tooltip = signal<{ x: number; y: number } | null>(null);

  total = computed(() => this.items().reduce((acc, it) => acc + (it.value || 0), 0));

  segments = computed<DonutSeg[]>(() => {
    const items = this.items();
    const total = this.total();
    const C = this.circumference;
    const nonZero = items.filter(it => (it.value || 0) > 0).length;
    const gap = nonZero > 1 ? 7 : 0;
    const gapDeg = (gap / C) * 360;

    let acc = 0;
    return items.map(it => {
      const value = it.value || 0;
      const frac = total > 0 ? value / total : 0;
      const arc = frac * C;
      const startFrac = total > 0 ? acc / total : 0;
      acc += value;
      const visible = value > 0 ? Math.max(arc - gap, 0.6) : 0;
      return {
        label: it.label,
        value,
        color: it.color,
        pct: total > 0 ? Math.round(frac * 100) : 0,
        isZero: value === 0,
        dash: `${visible} ${C}`,
        rotation: -90 + startFrac * 360 + gapDeg / 2
      };
    });
  });

  hovered = computed<DonutSeg | null>(() => {
    const i = this.hoverIndex();
    if (i === null) return null;
    return this.segments()[i] ?? null;
  });

  constructor() {
    // Difiere un frame para que la transición de stroke-dasharray dispare desde 0.
    afterNextRender(() => {
      requestAnimationFrame(() => this.mounted.set(true));
    });
  }

  /** dasharray animable: arranca "vacío" y crece a su longitud real al montar. */
  dashFor(seg: DonutSeg): string {
    return this.mounted() ? seg.dash : `0 ${this.circumference}`;
  }

  isDim(index: number): boolean {
    const hovered = this.hoverIndex();
    return hovered !== null && hovered !== index;
  }

  setHover(index: number): void {
    this.hoverIndex.set(index);
  }

  clearHover(): void {
    this.hoverIndex.set(null);
  }

  onMove(event: MouseEvent): void {
    const host = event.currentTarget as HTMLElement;
    const rect = host.getBoundingClientRect();
    this.tooltip.set({ x: event.clientX - rect.left, y: event.clientY - rect.top });
  }
}
