package com.antifakenews.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class NewsAnalysisRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public NewsAnalysisRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    /**
     * Carga, en una sola consulta, todos los indicadores numéricos que el
     * service necesita para aplicar las reglas de scoring.
     *
     * Devuelve Optional.empty() si la noticia no existe.
     */
    public Optional<AnalysisInputs> fetchSignals(String userId, String id) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $id})
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(src:Source)
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(:Claim)<-[:CHECKS]-(fc:FactCheck)
                  RETURN sum(CASE WHEN fc.verdict = 'FALSE'      THEN 1 ELSE 0 END) AS falseChecks,
                         sum(CASE WHEN fc.verdict = 'MISLEADING' THEN 1 ELSE 0 END) AS misleadingChecks
                }
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(c:Claim)-[:REFUTED_BY]->(:Evidence)
                  RETURN count(c) AS refutedClaims
                }
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(c:Claim)
                  WHERE NOT EXISTS { MATCH (c)-[:SUPPORTED_BY|REFUTED_BY]->(:Evidence) }
                  RETURN count(c) AS claimsWithoutEvidence
                }
                CALL (n) {
                  OPTIONAL MATCH (p:Post)-[s:SPREADS]->(n)
                  RETURN count(p) AS postCount, max(s.reach) AS maxReach
                }
                CALL (n) {
                  OPTIONAL MATCH (u1:User)-[:CREATED|SHARED]->(:Post)-[:SPREADS]->(n)
                                 <-[:SPREADS]-(:Post)<-[:CREATED|SHARED]-(u2:User)
                  WHERE u1.id < u2.id
                    AND EXISTS { MATCH (u1)-[:FOLLOWS|INTERACTS_WITH]-(u2) }
                  RETURN count(u1) AS connectedPairs
                }
                RETURN n.id   AS id,
                       n.title AS title,
                       src.credibilityScore AS sourceCredibility,
                       falseChecks, misleadingChecks, refutedClaims,
                       claimsWithoutEvidence, postCount, maxReach, connectedPairs
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("userId", userId, "id", id));
                if (!result.hasNext()) {
                    return Optional.<AnalysisInputs>empty();
                }
                Record r = result.next();
                return Optional.of(new AnalysisInputs(
                        r.get("id").asString(null),
                        r.get("title").asString(null),
                        r.get("sourceCredibility").isNull() ? null : r.get("sourceCredibility").asDouble(),
                        r.get("falseChecks").asLong(0L),
                        r.get("misleadingChecks").asLong(0L),
                        r.get("refutedClaims").asLong(0L),
                        r.get("claimsWithoutEvidence").asLong(0L),
                        r.get("postCount").asLong(0L),
                        r.get("maxReach").isNull() ? null : r.get("maxReach").asLong(),
                        r.get("connectedPairs").asLong(0L)
                ));
            });
        }
    }

    /**
     * Persiste el riskScore/riskLevel OFICIAL (única fuente de verdad) en :News,
     * con su procedencia. El análisis es determinístico: re-ejecutar produce el
     * mismo valor si el grafo no cambió.
     */
    public void persistRisk(String id, int score, String level) {
        final String cypher = """
                MATCH (n:News {id: $id})
                SET n.riskScore = $score,
                    n.riskLevel = $level,
                    n.status = 'ANALYZED',
                    n.riskCalculatedAt = datetime(),
                    n.riskCalculationSource = 'GRAPH_RULES',
                    n.riskCalculationVersion = 'v1'
                """;
        try (Session session = driver.session(sessionConfig)) {
            session.executeWrite(tx -> {
                tx.run(cypher, Map.of("id", id, "score", score, "level", level));
                return null;
            });
        }
    }

    public record AnalysisInputs(
            String id,
            String title,
            Double sourceCredibility,
            long falseChecks,
            long misleadingChecks,
            long refutedClaims,
            long claimsWithoutEvidence,
            long postCount,
            Long maxReach,
            long connectedPairs
    ) {}
}
