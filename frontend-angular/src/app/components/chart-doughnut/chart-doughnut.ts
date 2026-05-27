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
import { Chart, ChartConfiguration, Plugin } from 'chart.js';

import { registerCharts } from '../../core/chart/chart-register';
import { CHART_FONT_FAMILY, chartThemeColors, resolveColor } from '../../core/chart/chart-theme';
import { ThemeService } from '../../core/services/theme.service';

export interface DonutItem {
  label: string;
  value: number;
  /** Color del segmento (acepta `var(--x)` o un color concreto). */
  color: string;
}

/**
 * Doughnut Chart.js reutilizable, con estética NexoVeraz:
 * aro grueso, separación entre segmentos, total en el centro, tooltip oscuro
 * y animación de aparición. Se re-renderiza al cambiar el tema (dark/light).
 */
@Component({
  selector: 'nv-chart-doughnut',
  imports: [],
  templateUrl: './chart-doughnut.html',
  styleUrl: './chart-doughnut.scss'
})
export class ChartDoughnut implements OnDestroy {
  title = input<string>('');
  centerLabel = input<string>('');
  tooltipUnit = input<string>('');
  items = input<DonutItem[]>([]);
  emptyMessage = input<string>('Sin datos para mostrar');

  private themeService = inject(ThemeService);
  private canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('canvas');
  private chart: Chart<'doughnut'> | null = null;

  total = computed(() => this.items().reduce((acc, it) => acc + (it.value || 0), 0));
  hasData = computed(() => this.total() > 0);

  constructor() {
    registerCharts();
    // Re-renderiza cuando cambian los datos, aparece el canvas o cambia el tema.
    effect(() => {
      const canvas = this.canvasRef()?.nativeElement;
      const items = this.items();
      this.themeService.theme(); // dependencia: rebuild en toggle de tema
      if (!canvas || !this.hasData()) {
        this.destroyChart();
        return;
      }
      this.render(canvas, items);
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
   * resize responsive de Chart.js (que de lo contrario la cancela y deja el chart
   * estático). resize() fija el tamaño final, reset() vuelve al estado inicial y
   * update() anima hasta el estado final de forma visible.
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

  private render(canvas: HTMLCanvasElement, items: DonutItem[]): void {
    const theme = chartThemeColors();
    const total = this.total();
    const centerLabel = this.centerLabel();
    const tooltipUnit = this.tooltipUnit();

    // Plugin local: dibuja el total y la etiqueta en el centro del aro.
    const centerText: Plugin<'doughnut'> = {
      id: 'nvCenterText',
      afterDatasetsDraw: (chart) => {
        const arc = chart.getDatasetMeta(0)?.data?.[0] as unknown as
          | { x: number; y: number; innerRadius: number }
          | undefined;
        if (!arc) return;
        const { ctx } = chart;
        // Tamaños adaptativos al radio interior: grandes en desktop, se achican en mobile.
        const inner = arc.innerRadius || 60;
        const totalFont = Math.max(34, Math.min(44, Math.round(inner * 0.6)));
        const labelFont = Math.max(14, Math.min(18, Math.round(inner * 0.22)));

        ctx.save();
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = theme.text;
        ctx.font = `800 ${totalFont}px ${CHART_FONT_FAMILY}`;
        ctx.fillText(String(total), arc.x, arc.y - (centerLabel ? totalFont * 0.3 : 0));
        if (centerLabel) {
          ctx.fillStyle = theme.muted;
          ctx.font = `700 ${labelFont}px ${CHART_FONT_FAMILY}`;
          if ('letterSpacing' in ctx) {
            (ctx as CanvasRenderingContext2D & { letterSpacing: string }).letterSpacing = '1.5px';
          }
          ctx.fillText(centerLabel.toUpperCase(), arc.x, arc.y + totalFont * 0.46);
        }
        ctx.restore();
      }
    };

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: items.map(i => i.label),
        datasets: [
          {
            data: items.map(i => i.value || 0),
            backgroundColor: items.map(i => resolveColor(i.color)),
            borderColor: theme.card,
            borderWidth: 3,
            hoverOffset: 10,
            spacing: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '68%',
        // Despliegue circular en sentido horario (anima el barrido del aro + escala).
        animation: { animateRotate: true, animateScale: true, duration: 1600, easing: 'easeOutQuart' },
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
                const value = ctx.parsed || 0;
                const pct = total > 0 ? Math.round((value / total) * 100) : 0;
                const unit = tooltipUnit ? ` ${tooltipUnit}` : '';
                return ` ${ctx.label}: ${value}${unit} (${pct}%)`;
              }
            }
          }
        }
      },
      plugins: [centerText]
    };

    this.destroyChart();
    console.debug('[nv-chart-doughnut] crear chart', { total });
    this.chart = new Chart(canvas, config);
    this.playEntrance();
  }
}
