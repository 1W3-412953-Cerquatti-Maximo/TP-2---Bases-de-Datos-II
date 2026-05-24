import { RiskLevel } from './news.model';

export interface RiskSignal {
  code: string;
  description: string;
  points: number;
}

export interface NewsAnalysis {
  newsId: string;
  title: string;
  riskScore: number;
  riskLevel: RiskLevel;
  summary: string;
  signals: RiskSignal[];
}
