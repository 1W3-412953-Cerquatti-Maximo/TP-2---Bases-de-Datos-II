/**
 * Helpers para que Chart.js tome los colores del tema NexoVeraz (dark/light).
 * El canvas no resuelve `var(--x)`, así que leemos los valores reales del :root.
 */

/** Lee una CSS custom property del documento (ej: '--color-text'). */
export function cssVar(name: string): string {
  if (typeof document === 'undefined') return '';
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

/** Resuelve un color que puede venir como `var(--nombre)` a su valor concreto. */
export function resolveColor(color: string): string {
  const match = /^var\((--[A-Za-z0-9-]+)\)$/.exec((color ?? '').trim());
  if (match) {
    return cssVar(match[1]) || color;
  }
  return color;
}

/** Paleta de tooltip/ejes coherente con el tema actual. */
export function chartThemeColors() {
  return {
    text: cssVar('--color-text') || '#E2E8F0',
    muted: cssVar('--color-muted') || '#94A3B8',
    border: cssVar('--color-border') || '#1E293B',
    card: cssVar('--color-card') || '#111C2E',
    surface: cssVar('--color-surface') || '#0F172A'
  };
}

/** Stack de fuente sans concreto para dibujar texto en canvas. */
export const CHART_FONT_FAMILY =
  "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";
