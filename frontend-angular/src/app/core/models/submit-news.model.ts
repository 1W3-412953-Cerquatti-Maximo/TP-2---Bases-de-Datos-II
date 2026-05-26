import { RiskLevel } from './news.model';

export interface SubmitNewsUrlRequest {
  url: string;
  title?: string;
  content?: string;
  sourceName?: string;
  topicNames?: string[];
  riskScore?: number;
  riskLevel?: RiskLevel;
  evaluationSummary?: string;
}

export interface UrlExtractionResult {
  success: boolean;
  titleExtracted: boolean;
  descriptionExtracted: boolean;
  warnings: string[];
}

export interface SubmitNewsUrlResponse {
  newsId: string;
  title: string;
  url: string;
  sourceName: string;
  status: string;
  riskScore: number;
  riskLevel: RiskLevel;
  topicNames: string[];
  extraction: UrlExtractionResult;
}

export interface DeleteNewsResponse {
  newsId: string;
  deleted: boolean;
  sourceDeleted: boolean;
  sourceName: string | null;
}
