import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  signal
} from '@angular/core';
import type cytoscape from 'cytoscape';
import type { Core, ElementDefinition, NodeSingular, StylesheetJson } from 'cytoscape';

import { GRAPH_NODE_META, GRAPH_NODE_ORDER } from '../../core/graph/graph-node-meta';
import { GraphNode, GraphResponse } from '../../core/models/graph.model';

interface CytoscapeNodeData {
  id: string;
  label: string;
  displayLabel: string;
  title: string;
  shortTitle: string;
  color: string;
}

interface SelectedGraphNode {
  id: string;
  label: string;
  title: string;
  color: string;
  connections: number;
}

@Component({
  selector: 'nv-graph-viewer',
  templateUrl: './graph-viewer.html',
  styleUrl: './graph-viewer.scss'
})
export class GraphViewer implements AfterViewInit, OnChanges, OnDestroy {
  @Input() graph: GraphResponse | null = null;
  @ViewChild('graphHost') private graphHost?: ElementRef<HTMLDivElement>;

  readonly groupOrder = GRAPH_NODE_ORDER;
  readonly nodeMeta = GRAPH_NODE_META;
  readonly renderError = signal<string | null>(null);
  readonly selectedNode = signal<SelectedGraphNode | null>(null);

  private cy: Core | null = null;
  private cytoscapeLib: typeof cytoscape | null = null;
  private renderVersion = 0;
  private viewReady = false;

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderGraph();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['graph'] && this.viewReady) {
      this.renderGraph();
    }
  }

  ngOnDestroy(): void {
    this.destroyGraph();
  }

  get fallbackGroups(): Array<{ label: string; displayName: string; color: string; nodes: GraphNode[] }> {
    const graph = this.graph;
    if (!graph) return [];

    return this.groupOrder
      .map(label => ({
        label,
        displayName: this.nodeMeta[label].displayName,
        color: this.nodeMeta[label].color,
        nodes: graph.nodes.filter(node => node.label === label)
      }))
      .filter(group => group.nodes.length > 0);
  }

  private async renderGraph(): Promise<void> {
    const renderVersion = ++this.renderVersion;
    this.destroyGraph();
    this.selectedNode.set(null);
    this.renderError.set(null);

    const graph = this.graph;
    const host = this.graphHost?.nativeElement;
    if (!this.viewReady || !graph || !host || graph.nodes.length === 0) return;

    try {
      const cytoscapeLib = await this.loadCytoscape();
      if (renderVersion !== this.renderVersion) return;

      this.cy = cytoscapeLib({
        container: host,
        elements: this.toElements(graph),
        layout: {
          name: 'cose',
          animate: false,
          fit: true,
          padding: 32,
          nodeRepulsion: 180000,
          idealEdgeLength: 120
        },
        style: this.buildStyles(),
        minZoom: 0.35,
        maxZoom: 2.2,
        wheelSensitivity: 0.18
      });

      this.bindEvents();
      this.cy.ready(() => {
        this.cy?.fit(undefined, 32);
      });
    } catch (error) {
      console.error('GraphViewer render failed', error);
      this.renderError.set('No se pudo renderizar el grafo interactivo.');
    }
  }

  private bindEvents(): void {
    const cy = this.cy;
    if (!cy) return;

    cy.on('tap', 'node', event => {
      const node = event.target as NodeSingular;
      this.highlightNode(node);
    });

    cy.on('tap', event => {
      if (event.target === cy) {
        this.clearSelection();
      }
    });

    cy.on('mouseover', 'node', event => {
      event.target.addClass('hovered');
    });

    cy.on('mouseout', 'node', event => {
      event.target.removeClass('hovered');
    });
  }

  private highlightNode(node: NodeSingular): void {
    const cy = this.cy;
    if (!cy) return;

    cy.elements().removeClass('dimmed highlighted selected-node');
    cy.elements().addClass('dimmed');

    const neighborhood = node.closedNeighborhood();
    neighborhood.removeClass('dimmed').addClass('highlighted');
    node.removeClass('highlighted').addClass('selected-node');

    const data = node.data() as CytoscapeNodeData;
    this.selectedNode.set({
      id: data.id,
      label: data.displayLabel,
      title: data.title,
      color: data.color,
      connections: node.connectedEdges().length
    });
  }

  private clearSelection(): void {
    const cy = this.cy;
    if (!cy) return;

    cy.elements().removeClass('dimmed highlighted selected-node');
    this.selectedNode.set(null);
  }

  private toElements(graph: GraphResponse): ElementDefinition[] {
    const nodeIds = new Set(graph.nodes.map(node => node.id));

    const nodes: ElementDefinition[] = graph.nodes.map(node => {
      const meta = GRAPH_NODE_META[node.label];
      return {
        group: 'nodes',
        data: {
          id: node.id,
          label: node.label,
          displayLabel: meta.displayName,
          title: this.nodeTitle(node),
          shortTitle: this.truncateTitle(this.nodeTitle(node)),
          color: meta.color
        }
      };
    });

    const edges: ElementDefinition[] = graph.edges
      .filter(edge => nodeIds.has(edge.from) && nodeIds.has(edge.to))
      .map((edge, index) => ({
        group: 'edges',
        data: {
          id: `${edge.from}-${edge.type}-${edge.to}-${index}`,
          source: edge.from,
          target: edge.to,
          type: edge.type,
          properties: edge.properties
        }
      }));

    return [...nodes, ...edges];
  }

  private buildStyles(): StylesheetJson {
    const textColor = this.cssVar('--color-text', '#E5E7EB');
    const mutedColor = this.cssVar('--color-muted', '#94A3B8');
    const borderColor = this.cssVar('--color-border', '#334155');
    const surfaceColor = this.cssVar('--color-surface', '#0F172A');
    const accentColor = this.cssVar('--color-accent', '#22C55E');

    return [
      {
        selector: 'node',
        style: {
          'background-color': 'data(color)',
          label: 'data(shortTitle)',
          shape: 'round-rectangle',
          width: 'label',
          height: 'label',
          padding: '14px',
          color: textColor,
          'font-size': 11,
          'font-weight': 600,
          'text-wrap': 'wrap',
          'text-max-width': '120px',
          'text-valign': 'center',
          'text-halign': 'center',
          'border-width': 1,
          'border-color': surfaceColor,
          'text-outline-width': 0,
          'overlay-opacity': 0,
          'shadow-blur': 16,
          'shadow-opacity': 0.16,
          'shadow-color': '#020617',
          'shadow-offset-x': 0,
          'shadow-offset-y': 8
        }
      },
      {
        selector: 'node[label = "News"]',
        style: {
          shape: 'hexagon',
          'font-size': 12,
          'border-width': 2
        }
      },
      {
        selector: 'edge',
        style: {
          width: 2,
          'line-color': borderColor,
          'target-arrow-color': borderColor,
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier',
          opacity: 0.9
        }
      },
      {
        selector: '.highlighted',
        style: {
          'line-color': accentColor,
          'target-arrow-color': accentColor,
          'border-color': accentColor,
          'border-width': 2,
          opacity: 1,
          'z-index': 20
        }
      },
      {
        selector: '.selected-node',
        style: {
          'border-color': '#F8FAFC',
          'border-width': 3,
          'z-index': 30
        }
      },
      {
        selector: '.hovered',
        style: {
          'border-color': mutedColor,
          'border-width': 2
        }
      },
      {
        selector: '.dimmed',
        style: {
          opacity: 0.18
        }
      }
    ] as StylesheetJson;
  }

  private destroyGraph(): void {
    if (!this.cy) return;

    this.cy.destroy();
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

    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return value || fallback;
  }

  private nodeTitle(node: GraphNode): string {
    return node.title?.trim() || node.id;
  }

  private truncateTitle(title: string): string {
    return title.length > 48 ? `${title.slice(0, 45)}...` : title;
  }
}
