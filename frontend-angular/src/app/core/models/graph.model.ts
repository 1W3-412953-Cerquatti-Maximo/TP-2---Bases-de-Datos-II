export type GraphNodeLabel =
  | 'News'
  | 'Source'
  | 'Topic'
  | 'Claim'
  | 'Evidence'
  | 'FactCheck'
  | 'Post'
  | 'User';

export interface GraphNode {
  id: string;
  label: GraphNodeLabel;
  title: string;
}

export interface GraphEdge {
  from: string;
  to: string;
  type: string;
  properties: Record<string, unknown>;
}

export interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
}
