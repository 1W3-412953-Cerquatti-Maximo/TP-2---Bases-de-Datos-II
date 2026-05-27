import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip
} from 'chart.js';

let registered = false;

/**
 * Registra sólo los elementos de Chart.js que usa NexoVeraz (doughnut + stacked bar),
 * para mantener el bundle chico (tree-shaking). Idempotente.
 */
export function registerCharts(): void {
  if (registered) return;
  Chart.register(
    DoughnutController,
    ArcElement,
    BarController,
    BarElement,
    CategoryScale,
    LinearScale,
    Legend,
    Tooltip
  );
  registered = true;
}
