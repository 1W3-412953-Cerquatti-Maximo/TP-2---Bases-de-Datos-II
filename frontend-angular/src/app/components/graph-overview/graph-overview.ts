import {
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild
} from '@angular/core';
import type cytoscape from 'cytoscape';
import type { Core, ElementDefinition, LayoutOptions, NodeSingular, StylesheetJson } from 'cytoscape';

import { GRAPH_NODE_META } from '../../core/graph/graph-node-meta';
import { GraphNodeLabel } from '../../core/models/graph.model';
import { GraphSummaryResponse } from '../../core/models/dashboard.model';
import { ThemeService } from '../../core/services/theme.service';

interface SelectedNode {
  type: string;
  label: string;
  count: number;
  description: string;
  relations: string[];
  color: string;
}

interface LegendItem {
  type: string;
  label: string;
  color: string;
}

/** Descripción breve por tipo de entidad (lo que se muestra en el panel de detalle). */
const NODE_DESCRIPTIONS: Record<string, string> = {
  News: 'Noticias analizadas por el usuario dentro del sistema.',
  Source: 'Medios o sitios desde donde provienen las noticias.',
  Topic: 'Categorías temáticas asociadas a las noticias.',
  Claim: 'Afirmaciones detectadas dentro del contenido.',
  Evidence: 'Elementos usados para respaldar o refutar afirmaciones.',
  FactCheck: 'Verificaciones asociadas a las afirmaciones.',
  Post: 'Publicaciones que difundieron noticias.',
  User: 'Usuarios ficticios que participan en la circulación simulada.'
};

/** Relaciones conceptuales agregadas entre tipos (no relaciones reales individuales). */
const CONCEPT_EDGES: Array<[string, string]> = [
  ['News', 'Source'],
  ['News', 'Topic'],
  ['News', 'Claim'],
  ['News', 'Post'],
  ['Claim', 'Evidence'],
  ['Claim', 'FactCheck'],
  ['Post', 'User']
];

/**
 * Posiciones fijas (layout preset) para una distribución clara y legible:
 * Noticias al centro y el resto repartido alrededor. Cytoscape reescala con fit.
 */
const RING_RADIUS = 250;
const DIAG = Math.round(RING_RADIUS * 0.7071);
const NODE_POSITIONS: Record<string, { x: number; y: number }> = {
  News: { x: 0, y: 0 },                // centro
  Source: { x: 0, y: -RING_RADIUS },   // arriba
  Topic: { x: RING_RADIUS, y: 0 },     // derecha
  Claim: { x: DIAG, y: DIAG },         // abajo derecha
  Evidence: { x: 0, y: RING_RADIUS },  // abajo
  FactCheck: { x: -DIAG, y: DIAG },    // abajo izquierda
  Post: { x: -RING_RADIUS, y: 0 },     // izquierda
  User: { x: -DIAG, y: -DIAG }         // izquierda/arriba (junto a Posts)
};

function colorFor(type: string): string {
  return GRAPH_NODE_META[type as GraphNodeLabel]?.color ?? '#38BDF8';
}

/**
 * "Mapa general del grafo": vista agregada e interactiva del sistema (un nodo por
 * tipo de entidad), reutilizando Cytoscape.js (la misma librería del grafo de noticia).
 * Arrastrar nodos, zoom, pan, click con panel de detalle y botón de recentrado.
 */
@Component({
  selector: 'nv-graph-overview',
  imports: [],
  templateUrl: './graph-overview.html',
  styleUrl: './graph-overview.scss'
})
export class GraphOverview implements OnDestroy {
  data = input<GraphSummaryResponse | null>(null);

  private themeService = inject(ThemeService);
  private hostRef = viewChild<ElementRef<HTMLDivElement>>('graphHost');

  private cy: Core | null = null;
  private cytoscapeLib: typeof cytoscape | null = null;
  private renderVersion = 0;

  readonly selectedNode = signal<SelectedNode | null>(null);
  readonly renderError = signal<string | null>(null);

  hasData = computed(() => {
    const d = this.data();
    return !!d && d.nodes.some(n => (n.count || 0) > 0);
  });

  /** type → etiqueta para mostrar (a partir de los datos del backend). */
  private labelMap = computed<Record<string, string>>(() => {
    const map: Record<string, string> = {};
    for (const n of this.data()?.nodes ?? []) map[n.type] = n.label;
    return map;
  });

  legend = computed<LegendItem[]>(() =>
    (this.data()?.nodes ?? [])
      .filter(n => (n.count || 0) > 0)
      .map(n => ({ type: n.type, label: n.label, color: colorFor(n.type) }))
  );

  totalRelationships = computed(() => this.data()?.totalRelationships ?? 0);

  constructor() {
    effect(() => {
      const host = this.hostRef()?.nativeElement;
      const data = this.data();
      this.themeService.theme(); // rebuild al cambiar el tema (toma colores nuevos)
      if (!host || !data || !this.hasData()) {
        this.destroyGraph();
        return;
      }
      void this.renderGraph(host, data);
    });
  }

  ngOnDestroy(): void {
    this.destroyGraph();
  }

  /** Botón "Centrar grafo": reencuadra todos los nodos. */
  recenter(): void {
    const cy = this.cy;
    if (!cy) return;
    cy.animate({ fit: { eles: cy.elements(), padding: 40 }, duration: 350, easing: 'ease-out' });
  }

  private async renderGraph(host: HTMLDivElement, data: GraphSummaryResponse): Promise<void> {
    const version = ++this.renderVersion;
    this.destroyGraph();
    this.selectedNode.set(null);
    this.renderError.set(null);

    try {
      const cytoscapeLib = await this.loadCytoscape();
      if (version !== this.renderVersion) return;

      const layout = {
        name: 'preset',
        padding: 48,
        fit: true,
        animate: false
      } as unknown as LayoutOptions;

      this.cy = cytoscapeLib({
        container: host,
        elements: this.buildElements(data),
        style: this.buildStyles(),
        layout,
        minZoom: 0.4,
        maxZoom: 2.4,
        wheelSensitivity: 0.2,
        boxSelectionEnabled: false
      });

      this.bindEvents();
      this.cy.ready(() => this.cy?.fit(undefined, 40));
    } catch (err) {
      console.error('[nv-graph-overview] render falló', err);
      this.renderError.set('No se pudo renderizar el mapa del grafo.');
    }
  }

  private bindEvents(): void {
    const cy = this.cy;
    if (!cy) return;

    cy.on('tap', 'node', event => this.selectNode(event.target as NodeSingular));
    cy.on('tap', event => {
      if (event.target === cy) this.clearSelection();
    });
    cy.on('mouseover', 'node', event => event.target.addClass('hovered'));
    cy.on('mouseout', 'node', event => event.target.removeClass('hovered'));
  }

  private selectNode(node: NodeSingular): void {
    const cy = this.cy;
    if (!cy) return;

    cy.elements().removeClass('dimmed highlighted selected-node');
    cy.elements().addClass('dimmed');
    node.closedNeighborhood().removeClass('dimmed').addClass('highlighted');
    node.addClass('selected-node');

    const type = node.data('type') as string;
    const labels = this.labelMap();
    const relations = [
      ...new Set(
        CONCEPT_EDGES
          .filter(([a, b]) => a === type || b === type)
          .map(([a, b]) => (a === type ? b : a))
          .filter(t => labels[t])
          .map(t => labels[t])
      )
    ];

    this.selectedNode.set({
      type,
      label: node.data('label') as string,
      count: node.data('count') as number,
      description: NODE_DESCRIPTIONS[type] ?? '',
      relations,
      color: node.data('color') as string
    });
  }

  private clearSelection(): void {
    this.cy?.elements().removeClass('dimmed highlighted selected-node');
    this.selectedNode.set(null);
  }

  private buildElements(data: GraphSummaryResponse): ElementDefinition[] {
    const present = data.nodes.filter(n => (n.count || 0) > 0);
    const types = new Set(present.map(n => n.type));
    const maxCount = Math.max(...present.map(n => n.count || 0), 1);

    const nodes: ElementDefinition[] = present.map(n => {
      const isNews = n.type === 'News';
      const ratio = Math.sqrt((n.count || 0) / maxCount);
      // Bases altas para que se lean sin zoom; News claramente protagonista.
      const size = isNews
        ? 94 + Math.round(ratio * 16)   // central: 94–110
        : 58 + Math.round(ratio * 22);  // secundarios: 58–80
      return {
        group: 'nodes',
        position: { ...(NODE_POSITIONS[n.type] ?? { x: 0, y: 0 }) },
        data: {
          id: n.type,
          type: n.type,
          label: n.label,
          count: n.count,
          text: `${n.label}\n${n.count}`,
          color: colorFor(n.type),
          size,
          isNews: isNews ? 1 : 0
        }
      };
    });

    const edges: ElementDefinition[] = CONCEPT_EDGES
      .filter(([a, b]) => types.has(a) && types.has(b))
      .map(([a, b]) => ({ group: 'edges', data: { id: `${a}-${b}`, source: a, target: b } }));

    return [...nodes, ...edges];
  }

  private buildStyles(): StylesheetJson {
    const text = this.cssVar('--color-text', '#E2E8F0');
    const muted = this.cssVar('--color-muted', '#94A3B8');
    const border = this.cssVar('--color-border', '#1E293B');
    const surface = this.cssVar('--color-surface', '#0F172A');
    const accent = this.cssVar('--color-accent', '#38BDF8');

    return [
      {
        selector: 'node',
        style: {
          'background-color': 'data(color)',
          width: 'data(size)',
          height: 'data(size)',
          label: 'data(text)',
          shape: 'ellipse',
          color: text,
          'font-size': 16,
          'font-weight': 700,
          'text-wrap': 'wrap',
          'text-max-width': '110px',
          'text-valign': 'center',
          'text-halign': 'center',
          'border-width': 2,
          'border-color': surface,
          'overlay-opacity': 0,
          'shadow-blur': 18,
          'shadow-opacity': 0.18,
          'shadow-color': '#020617',
          'shadow-offset-x': 0,
          'shadow-offset-y': 8
        }
      },
      {
        selector: 'node[isNews = 1]',
        style: { shape: 'hexagon', 'border-width': 3, 'border-color': accent, 'font-size': 18 }
      },
      {
        selector: 'edge',
        style: {
          width: 3.5,
          'line-color': border,
          'target-arrow-color': border,
          'target-arrow-shape': 'triangle',
          'arrow-scale': 1,
          'curve-style': 'bezier',
          opacity: 0.85
        }
      },
      { selector: '.hovered', style: { 'border-color': muted, 'border-width': 3 } },
      {
        selector: '.highlighted',
        style: { 'line-color': accent, 'target-arrow-color': accent, opacity: 1, 'z-index': 20 }
      },
      { selector: '.selected-node', style: { 'border-color': '#F8FAFC', 'border-width': 4, 'z-index': 30 } },
      { selector: '.dimmed', style: { opacity: 0.18 } }
    ] as StylesheetJson;
  }

  private destroyGraph(): void {
    this.cy?.destroy();
    this.cy = null;
  }

  private async loadCytoscape(): Promise<typeof cytoscape> {
    if (this.cytoscapeLib) return this.cytoscapeLib;
    const module = await import('cytoscape');
    this.cytoscapeLib = module.default;
    return this.cytoscapeLib;
  }

  private cssVar(name: string, fallback: string): string {
    if (typeof document === 'undefined') return fallback;
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
  }
}
