export interface AiAnalyzeNewsRequest {
  title: string;
  content: string;
  url?: string | null;
  sourceName?: string | null;
  topicNames?: string[];
  riskScore?: number | null;
  riskLevel?: string | null;
}

export interface AiUsage {
  inputTokens: number;
  outputTokens: number;
}

export interface AiAnalyzeNewsResponse {
  enabled: boolean;
  ok: boolean;
  provider: string;
  model?: string;
  message?: string;
  summary?: string;
  riskAnalysis?: string;
  aiRiskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
  aiRiskScore: number;
  warningSignals: string[];
  recommendations: string[];
  limitations: string[];
  confidence: number;
  usage?: AiUsage;
}
