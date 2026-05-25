import { GraphNodeLabel } from '../models/graph.model';

export interface GraphNodeMeta {
  displayName: string;
  color: string;
}

export const GRAPH_NODE_ORDER: GraphNodeLabel[] = [
  'News', 'Source', 'Topic', 'Claim', 'Evidence', 'FactCheck', 'Post', 'User'
];

export const GRAPH_NODE_META: Record<GraphNodeLabel, GraphNodeMeta> = {
  News: { displayName: 'Noticia', color: '#14B8A6' },
  Source: { displayName: 'Fuente', color: '#38BDF8' },
  Topic: { displayName: 'Temas', color: '#F59E0B' },
  Claim: { displayName: 'Claims', color: '#EF4444' },
  Evidence: { displayName: 'Evidencias', color: '#22C55E' },
  FactCheck: { displayName: 'Fact Checks', color: '#A78BFA' },
  Post: { displayName: 'Posts', color: '#06B6D4' },
  User: { displayName: 'Usuarios', color: '#EC4899' }
};
