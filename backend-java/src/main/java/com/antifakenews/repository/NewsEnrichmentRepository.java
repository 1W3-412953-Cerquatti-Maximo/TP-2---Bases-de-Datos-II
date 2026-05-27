package com.antifakenews.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Persiste el enriquecimiento IA de una noticia en Neo4j de forma idempotente.
 *
 * Antes de insertar limpia SOLO los datos origin='AI_ENRICHMENT' de ESA News
 * (sus claims/evidencias/fact checks de IA y las relaciones ABOUT de IA). No
 * toca seed/manual ni nodos Topic compartidos.
 */
@Repository
public class NewsEnrichmentRepository {

    /** Tipos de relación válidos Claim->Evidence (validados en el servicio; seguros para interpolar). */
    private static final Set<String> EVIDENCE_RELS = Set.of("SUPPORTED_BY", "REFUTED_BY", "HAS_EVIDENCE_GAP");

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public NewsEnrichmentRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public Optional<PersistResult> persist(
            String userId, String newsId,
            boolean updatePublishedAt, String publishedAtIso,
            String provider, String model,
            List<Map<String, Object>> topics,
            List<Map<String, Object>> claims,
            List<Map<String, Object>> evidences,
            List<Map<String, Object>> factChecks) {

        try (Session session = driver.session(sessionConfig)) {
            return session.executeWrite(tx -> {
                // 1. Verificar pertenencia (defensa adicional; el servicio ya validó vía findById).
                Result owned = tx.run(
                        "MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $newsId}) RETURN n.id AS id",
                        Map.of("userId", userId, "newsId", newsId));
                if (!owned.hasNext()) {
                    return Optional.<PersistResult>empty();
                }

                // 2. Limpieza idempotente del enriquecimiento IA previo de ESTA noticia.
                tx.run("""
                        MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $newsId})
                        OPTIONAL MATCH (n)-[:CONTAINS]->(c:Claim {origin: 'AI_ENRICHMENT'})
                        OPTIONAL MATCH (c)-[:SUPPORTED_BY|REFUTED_BY|HAS_EVIDENCE_GAP]->(e:Evidence {origin: 'AI_ENRICHMENT'})
                        OPTIONAL MATCH (c)<-[:CHECKS]-(fc:FactCheck {origin: 'AI_ENRICHMENT'})
                        DETACH DELETE e, fc, c
                        WITH n
                        OPTIONAL MATCH (n)-[ab:ABOUT]->(:Topic) WHERE ab.source = 'AI_ENRICHMENT'
                        DELETE ab
                        """, Map.of("userId", userId, "newsId", newsId));

                // 3. Actualizar props de la News (+ publishedAt condicional).
                Map<String, Object> newsParams = new java.util.HashMap<>();
                newsParams.put("newsId", newsId);
                newsParams.put("provider", provider);
                newsParams.put("model", model);
                newsParams.put("update", updatePublishedAt);
                newsParams.put("pubIso", publishedAtIso);
                Result upd = tx.run("""
                        MATCH (n:News {id: $newsId})
                        WITH n, ($update AND $pubIso IS NOT NULL
                                 AND (n.publishedAt IS NULL OR n.publishedAt = n.createdAt)) AS doUpdate
                        SET n.aiEnrichedAt = datetime(),
                            n.aiEnrichmentProvider = $provider,
                            n.aiEnrichmentModel = $model,
                            n.aiEnrichmentStatus = 'COMPLETED'
                        FOREACH (_ IN CASE WHEN doUpdate THEN [1] ELSE [] END |
                            SET n.publishedAt = datetime($pubIso))
                        RETURN doUpdate AS publishedAtUpdated
                        """, newsParams);
                boolean publishedAtUpdated = upd.hasNext() && upd.next().get("publishedAtUpdated").asBoolean(false);

                // 4. Topics (MERGE por nombre; no pisa props de Topics existentes/seed).
                if (!topics.isEmpty()) {
                    tx.run("""
                            MATCH (n:News {id: $newsId})
                            UNWIND $topics AS t
                            MERGE (tp:Topic {name: t.name})
                              ON CREATE SET tp.id = randomUUID(), tp.slug = t.slug,
                                            tp.origin = 'AI_ENRICHMENT', tp.createdAt = datetime()
                            MERGE (n)-[r:ABOUT]->(tp)
                            SET r.relevance = t.relevance, r.confidence = t.confidence,
                                r.source = 'AI_ENRICHMENT', r.generatedAt = datetime()
                            """, Map.of("newsId", newsId, "topics", topics));
                }

                // 5. Claims (CREATE nuevos, exclusivos de esta News).
                if (!claims.isEmpty()) {
                    tx.run("""
                            MATCH (n:News {id: $newsId})
                            UNWIND $claims AS c
                            CREATE (cl:Claim {
                              id: c.id, text: c.text, status: 'AI_EXTRACTED', type: c.type,
                              riskLevel: c.riskLevel, confidence: c.confidence, explanation: c.explanation,
                              origin: 'AI_ENRICHMENT', createdAt: datetime()
                            })
                            CREATE (n)-[:CONTAINS {extractedAt: datetime(), extractionMethod: 'ANTHROPIC', confidence: c.confidence}]->(cl)
                            """, Map.of("newsId", newsId, "claims", claims));
                }

                // 6. Evidencias (el tipo de relación varía según el kind; conjunto cerrado y validado).
                for (Map<String, Object> ev : evidences) {
                    String relType = String.valueOf(ev.get("relType"));
                    if (!EVIDENCE_RELS.contains(relType)) {
                        relType = "HAS_EVIDENCE_GAP";
                    }
                    tx.run("""
                            MATCH (cl:Claim {id: $claimId})
                            CREATE (e:Evidence {
                              id: $id, description: $description, type: $kind, kind: $kind, url: null,
                              confidence: $confidence, explanation: $explanation,
                              origin: 'AI_ENRICHMENT', createdAt: datetime()
                            })
                            CREATE (cl)-[:%s {confidence: $confidence, note: $explanation, generatedAt: datetime(), source: 'AI_ENRICHMENT'}]->(e)
                            """.formatted(relType), ev);
                }

                // 7. Fact Checks (verificación asistida; CHECKS hacia el claim).
                if (!factChecks.isEmpty()) {
                    Map<String, Object> fcParams = Map.of(
                            "factChecks", factChecks, "provider", provider, "model", model);
                    tx.run("""
                            UNWIND $factChecks AS f
                            MATCH (cl:Claim {id: f.claimId})
                            CREATE (fc:FactCheck {
                              id: f.id, verdict: f.verdict, explanation: f.explanation, confidence: f.confidence,
                              publishedAt: datetime(), checkedAt: datetime(),
                              origin: 'AI_ENRICHMENT', provider: $provider, model: $model
                            })
                            CREATE (fc)-[:CHECKS {checkedAt: datetime(), method: 'AI_ASSISTED', confidence: f.confidence}]->(cl)
                            """, fcParams);
                }

                return Optional.of(new PersistResult(
                        topics.size(), claims.size(), evidences.size(), factChecks.size(), publishedAtUpdated));
            });
        }
    }

    /** Marca la noticia como enriquecimiento fallido (sin tocar datos previos). */
    public void markFailed(String userId, String newsId, String shortError) {
        try (Session session = driver.session(sessionConfig)) {
            session.executeWrite((TransactionContext tx) -> {
                tx.run("""
                        MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $newsId})
                        SET n.aiEnrichmentStatus = 'FAILED', n.aiEnrichmentError = $err, n.aiEnrichedAt = datetime()
                        """, Map.of("userId", userId, "newsId", newsId, "err", shortError == null ? "" : shortError));
                return null;
            });
        }
    }

    public record PersistResult(
            int topicsCreated,
            int claimsCreated,
            int evidencesCreated,
            int factChecksCreated,
            boolean publishedAtUpdated
    ) {}
}
