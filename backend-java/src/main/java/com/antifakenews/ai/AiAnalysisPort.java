package com.antifakenews.ai;

import com.antifakenews.dto.AiAnalyzeNewsRequest;
import com.antifakenews.dto.AiAnalyzeNewsResponse;

/**
 * Puerto de análisis asistido por IA.
 *
 * La IA es un AYUDANTE opcional: resume, sugiere claims y temas y genera
 * advertencias preliminares. NO decide si una noticia es falsa, NO calcula el
 * riskScore final y NO reemplaza el análisis determinístico del grafo Neo4j.
 */
public interface AiAnalysisPort {

    AiAnalyzeNewsResponse analyze(AiAnalyzeNewsRequest request);
}
