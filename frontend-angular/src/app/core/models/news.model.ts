import { Source } from './source.model';
import { Topic } from './topic.model';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface NewsSummary {
  id: string;
  title: string;
  url: string;
  publishedAt: string | null;
  createdAt: string | null;
  status: string | null;
  riskScore: number | null;
  riskLevel: RiskLevel | null;
  sourceName: string | null;
  topicNames: string[];
}

export interface Claim {
  id: string;
  text: string;
  status: string;
}

export interface Evidence {
  id: string;
  description: string;
  type: string;
  url: string;
}

export interface FactCheck {
  id: string;
  verdict: string;
  explanation: string;
  confidence: number | null;
  publishedAt: string | null;
}

export interface Post {
  id: string;
  content: string;
  platform: string;
  createdAt: string | null;
}

export interface User {
  id: string;
  username: string;
  role: string;
}

export interface NewsDetail {
  id: string;
  title: string;
  content: string;
  url: string;
  publishedAt: string | null;
  createdAt: string | null;
  publishedAtSource: string | null;
  publishedAtConfidence: number | null;
  status: string | null;
  riskScore: number | null;
  riskLevel: RiskLevel | null;
  source: Source | null;
  topics: Topic[];
  claims: Claim[];
  evidence: Evidence[];
  factChecks: FactCheck[];
  posts: Post[];
  users: User[];
}
