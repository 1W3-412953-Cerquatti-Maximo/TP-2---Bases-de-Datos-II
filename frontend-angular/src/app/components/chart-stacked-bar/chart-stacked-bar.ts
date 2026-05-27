import {
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  viewChild
} from '@angular/core';
import { Chart, ChartConfiguration } from 'chart.js';

import { registerCharts } from '../../core/chart/chart-register';
import { chartThemeColors, resolveColor } from '../../core/chart/chart-theme';
import { ThemeService } from '../../core/services/theme.service';

export interface TopicRiskDatum {
  topic: string;
  low: number;
  medium: number;
  high: number;
}

/** Parte etiquetas largas en varias líneas (Chart.js renderiza arrays como multilínea). */
function wrapLabel(label: string, maxChars = 12): string | string[] {
  if (!label || label.length <= maxChars) return label;
  const words = label.split(' ');
  const lines: string[] = [];
  let current = '';
  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (candidate.length > maxChars && current) {
      lines.push(current);
      current = word;
    } else {
      current = candidate;
    }
  }
  if (current) lines.push(current);
  return lines;
}

/**
 * Stacked Bar Chart.js: cantidad de noticias por tema, apiladas por nivel de riesgo.
 * Estética NexoVeraz (tooltip oscuro, ejes con buen contraste en dark/light,
 * animación de crecimiento). Se re-renderiza al cambiar el tema.
 */
@Component({
  selector: 'nv-chart-stacked-bar',
  imports: [],
  templateUrl: './chart-stacked-bar.html',
  styleUrl: './chart-stacked-bar.scss'
})
export class ChartStackedBar implements OnDestroy {
  title = input<string>('');
  data = input<TopicRiskDatum[]>([]);
  emptyMessage = input<string>('Sin datos para mostrar');

  private themeService = inject(ThemeService);
  private canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('canvas');
  private chart: Chart<'bar'> | null = null;

  hasData = computed(() =>
    this.data().some(d => (d.low || 0) + (d.medium || 0) + (d.high || 0) > 0)
  );

  constructor() {
    registerCharts();
    effect(() => {
      const canvas = this.canvasRef()?.nativeElement;
      const data = this.data();
      this.themeService.theme(); // rebuild en toggle de tema
      if (!canvas || !this.hasData()) {
        this.destroyChart();
        return;
      }
      this.render(canvas, data);
    });
  }

  ngOnDestroy(): void {
    this.destroyChart();
  }

  private destroyChart(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  /**
   * Re-dispara la animación de entrada en el próximo frame, ya pasado el primer
   * resize responsive de Chart.js (que de lo contrario la cancela y deja las barras
   * estáticas). resize() fija el tamaño final, reset() lleva las barras a altura 0
   * y update() las hace crecer desde la base de forma visible.
   */
  private playEntrance(): void {
    requestAnimationFrame(() => {
      const chart = this.chart;
      if (!chart) return;
      chart.resize();
      chart.reset();
      chart.update();
    });
  }

  private render(canvas: HTMLCanvasElement, data: TopicRiskDatum[]): void {
    const theme = chartThemeColors();
    const colorLow = resolveColor('var(--color-low)');
    const colorMedium = resolveColor('var(--color-medium)');
    const colorHigh = resolveColor('var(--color-high)');

    const baseBar = {
      borderColor: theme.card,
      borderWidth: 1,
      borderRadius: 4,
      borderSkipped: false as const,
      maxBarThickness: 56
    };

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: data.map(d => d.topic),
        datasets: [
          { label: 'Bajo', data: data.map(d => d.low || 0), backgroundColor: colorLow, ...baseBar },
          { label: 'Medio', data: data.map(d => d.medium || 0), backgroundColor: colorMedium, ...baseBar },
          { label: 'Alto', data: data.map(d => d.high || 0), backgroundColor: colorHigh, ...baseBar }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        // Las barras crecen desde la base (animación de altura) con delay progresivo
        // por columna (izq → der) para un efecto de "levantarse" escalonado.
        animation: {
          duration: 1500,
          easing: 'easeOutQuart',
          delay: (ctx: { type: string; mode: string; dataIndex: number }) =>
            ctx.type === 'data' && ctx.mode === 'default' ? ctx.dataIndex * 60 : 0
        },
        interaction: { mode: 'index', intersect: false },
        scales: {
          x: {
            stacked: true,
            ticks: {
              color: theme.muted,
              font: { size: 13, weight: 600 },
              autoSkip: false,
              maxRotation: 45,
              minRotation: 0,
              // Etiquetas largas (ej. "Cambio Climático") se parten en varias líneas.
              callback(value) {
                return wrapLabel((this as { getLabelForValue(v: number): string }).getLabelForValue(value as number));
              }
            },
            grid: { display: false },
            border: { color: theme.border }
          },
          y: {
            stacked: true,
            beginAtZero: true,
            ticks: { color: theme.muted, precision: 0, font: { size: 13 } },
            grid: { color: theme.border },
            border: { color: theme.border }
          }
        },
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: theme.text,
              usePointStyle: true,
              pointStyle: 'circle',
              padding: 20,
              boxWidth: 14,
              boxHeight: 14,
              font: { size: 15, weight: 600 }
            }
          },
          tooltip: {
            backgroundColor: theme.surface,
            titleColor: theme.text,
            bodyColor: theme.text,
            borderColor: theme.border,
            borderWidth: 1,
            padding: 13,
            cornerRadius: 10,
            usePointStyle: true,
            titleFont: { size: 15, weight: 700 },
            bodyFont: { size: 14, weight: 500 },
            boxPadding: 6,
            callbacks: {
              label: (ctx) => {
                const value = ctx.parsed.y || 0;
                return ` ${ctx.dataset.label}: ${value} ${value === 1 ? 'noticia' : 'noticias'}`;
              }
            }
          }
        }
      }
    };

    this.destroyChart();
    console.debug('[nv-chart-stacked-bar] crear chart', { temas: data.length });
    this.chart = new Chart(canvas, config);
    this.playEntrance();
  }
}
