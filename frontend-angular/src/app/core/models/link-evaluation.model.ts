import { RiskSignal } from './analysis.model';
import { RiskLevel } from './news.model';

export interface EvaluateLinkRequest {
  url: string;
}

export interface CredibilityDiagnosis {
  riskScore: number;
  riskLevel: RiskLevel;
  summary: string;
  signals: RiskSignal[];
  preliminary: boolean;
  basis: string;
}

export interface EvaluateLinkResponse {
  url: string;
  resolvedUrl: string;
  title: string;
  contentPreview: string;
  fetchStatus: string;
  credibilityDiagnosis: CredibilityDiagnosis;
  warnings: string[];
}
