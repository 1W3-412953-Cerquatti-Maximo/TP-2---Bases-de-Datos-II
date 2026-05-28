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
  // Subfase B: resultado del pipeline de procesamiento (enriquecimiento IA).
  aiEnrichmentStatus?: string;
  topicsCount?: number;
  claimsCount?: number;
  evidencesCount?: number;
  factChecksCount?: number;
  warnings?: string[];
  // Dedup: backend respondió con una News ya existente; no se ejecutó pipeline.
  alreadyExists?: boolean;
  message?: string;
}

export interface DeleteNewsResponse {
  newsId: string;
  deleted: boolean;
  sourceDeleted: boolean;
  sourceName: string | null;
}
