export interface AiAnalyzeNewsRequest {
  title: string;
  content: string;
}

export interface AiAnalyzeNewsResponse {
  enabled: boolean;
  provider: string;
  summary: string;
  suggestedClaims: string[];
  suggestedTopics: string[];
  warnings: string[];
}
