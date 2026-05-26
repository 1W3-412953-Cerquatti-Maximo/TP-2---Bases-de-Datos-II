export interface DashboardSummary {
  totalNews: number;
  highRiskNews: number;
  mediumRiskNews: number;
  lowRiskNews: number;
  totalSources: number;
  highCredibilitySources: number;
  mediumCredibilitySources: number;
  lowCredibilitySources: number;
  totalClaims: number;
  totalFactChecks: number;
  totalPosts: number;
  totalUsers: number;
}

export interface TopicRiskRankingItem {
  topic: string;
  avgRiskScore: number;
  newsCount: number;
}

export interface RiskSignalSummaryItem {
  code: string;
  label: string;
  count: number;
}

export interface NewsTimelineItem {
  date: string;
  low: number;
  medium: number;
  high: number;
  total: number;
}

export interface GraphSummaryNode {
  label: string;
  count: number;
  type: string;
}

export interface GraphSummaryResponse {
  nodes: GraphSummaryNode[];
  totalRelationships: number;
}
