package com.antifakenews.repository;

import com.antifakenews.dto.DashboardSummaryDto;
import com.antifakenews.dto.GraphSummaryDto;
import com.antifakenews.dto.GraphSummaryNodeDto;
import com.antifakenews.dto.NewsTimelineDto;
import com.antifakenews.dto.TopicRiskRankingDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public DashboardRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    /**
     * Conteos del subgrafo del usuario: solo las noticias que posee (OWNS_NEWS)
     * y las entidades alcanzables desde ellas. Una cuenta sin noticias da todo en 0.
     */
    public DashboardSummaryDto getSummary(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})
                CALL (owner) {
                  OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(n:News)
                  RETURN count(n) AS totalNews,
                         sum(CASE WHEN n.riskLevel = 'HIGH'   THEN 1 ELSE 0 END) AS highRiskNews,
                         sum(CASE WHEN n.riskLevel = 'MEDIUM' THEN 1 ELSE 0 END) AS mediumRiskNews,
                         sum(CASE WHEN n.riskLevel = 'LOW'    THEN 1 ELSE 0 END) AS lowRiskNews
                }
                CALL (owner) {
                  OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:PUBLISHED_BY]->(s:Source)
                  WITH collect(DISTINCT s) AS sources
                  RETURN size(sources) AS totalSources,
                         size([x IN sources WHERE x.credibilityScore >= 0.7]) AS highCredibilitySources,
                         size([x IN sources WHERE x.credibilityScore >= 0.4 AND x.credibilityScore < 0.7]) AS mediumCredibilitySources,
                         size([x IN sources WHERE x.credibilityScore IS NULL OR x.credibilityScore < 0.4]) AS lowCredibilitySources
                }
                CALL (owner) {
                  OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:CONTAINS]->(c:Claim)
                  RETURN count(DISTINCT c) AS totalClaims
                }
                CALL (owner) {
                  OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:CONTAINS]->(:Claim)<-[:CHECKS]-(fc:FactCheck)
                  RETURN count(DISTINCT fc) AS totalFactChecks
                }
                CALL (owner) {
                  OPTIONAL MATCH (p:Post)-[:SPREADS]->(:News)<-[:OWNS_NEWS]-(owner)
                  RETURN count(DISTINCT p) AS totalPosts
                }
                CALL (owner) {
                  OPTIONAL MATCH (u:User)-[:CREATED|SHARED]->(:Post)-[:SPREADS]->(:News)<-[:OWNS_NEWS]-(owner)
                  RETURN count(DISTINCT u) AS totalUsers
                }
                RETURN totalNews, highRiskNews, mediumRiskNews, lowRiskNews,
                       totalSources, highCredibilitySources, mediumCredibilitySources, lowCredibilitySources,
                       totalClaims, totalFactChecks, totalPosts, totalUsers
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Record r = tx.run(cypher, Map.of("userId", userId)).single();
                return new DashboardSummaryDto(
                        r.get("totalNews").asLong(0L),
                        r.get("highRiskNews").asLong(0L),
                        r.get("mediumRiskNews").asLong(0L),
                        r.get("lowRiskNews").asLong(0L),
                        r.get("totalSources").asLong(0L),
                        r.get("highCredibilitySources").asLong(0L),
                        r.get("mediumCredibilitySources").asLong(0L),
                        r.get("lowCredibilitySources").asLong(0L),
                        r.get("totalClaims").asLong(0L),
                        r.get("totalFactChecks").asLong(0L),
                        r.get("totalPosts").asLong(0L),
                        r.get("totalUsers").asLong(0L)
                );
            });
        }
    }

    /**
     * Top temas por riesgo promedio del usuario. Promedia News.riskScore sobre las
     * noticias del usuario agrupadas por tema, ordena descendente y limita a 5.
     */
    public List<TopicRiskRankingDto> topicRiskRanking(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News)-[:ABOUT]->(t:Topic)
                WITH t, avg(coalesce(n.riskScore, 0)) AS avgScore, count(DISTINCT n) AS newsCount
                RETURN t.name AS topic, avgScore, newsCount
                ORDER BY avgScore DESC, newsCount DESC, topic ASC
                LIMIT 5
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("userId", userId)).list(r ->
                    new TopicRiskRankingDto(
                            r.get("topic").asString(null),
                            Math.round(r.get("avgScore").asDouble(0.0)),
                            r.get("newsCount").asLong(0L)
                    )));
        }
    }

    /**
     * Cuenta, sobre las noticias del usuario, en cuántas aparece cada señal de
     * riesgo. Reusa las mismas reglas/umbrales que NewsAnalysisService.
     * Devuelve un mapa code -> cantidad (los labels y el orden los pone el service).
     */
    public Map<String, Long> riskSignalCounts(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News)
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
                WITH src.credibilityScore AS cred,
                     falseChecks, misleadingChecks, refutedClaims, claimsWithoutEvidence,
                     postCount, maxReach, connectedPairs
                RETURN
                  sum(CASE WHEN cred IS NOT NULL AND cred < 0.4 THEN 1 ELSE 0 END) AS LOW_CREDIBILITY_SOURCE,
                  sum(CASE WHEN falseChecks > 0 THEN 1 ELSE 0 END)                  AS FALSE_FACT_CHECK,
                  sum(CASE WHEN misleadingChecks > 0 THEN 1 ELSE 0 END)             AS MISLEADING_FACT_CHECK,
                  sum(CASE WHEN refutedClaims > 0 THEN 1 ELSE 0 END)                AS CLAIM_REFUTED_BY_EVIDENCE,
                  sum(CASE WHEN claimsWithoutEvidence > 0 THEN 1 ELSE 0 END)        AS CLAIM_WITHOUT_EVIDENCE,
                  sum(CASE WHEN postCount > 3 THEN 1 ELSE 0 END)                    AS HIGH_PROPAGATION_VOLUME,
                  sum(CASE WHEN connectedPairs > 0 THEN 1 ELSE 0 END)               AS CONNECTED_USERS_PROPAGATION,
                  sum(CASE WHEN maxReach IS NOT NULL AND maxReach > 20000 THEN 1 ELSE 0 END) AS HIGH_REACH_POST
                """;

        String[] codes = {
                "LOW_CREDIBILITY_SOURCE", "FALSE_FACT_CHECK", "MISLEADING_FACT_CHECK",
                "CLAIM_REFUTED_BY_EVIDENCE", "CLAIM_WITHOUT_EVIDENCE", "HIGH_PROPAGATION_VOLUME",
                "CONNECTED_USERS_PROPAGATION", "HIGH_REACH_POST"
        };

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Map<String, Long> counts = new LinkedHashMap<>();
                var result = tx.run(cypher, Map.of("userId", userId));
                if (!result.hasNext()) {
                    for (String code : codes) counts.put(code, 0L);
                    return counts;
                }
                Record r = result.next();
                for (String code : codes) {
                    counts.put(code, r.get(code).asLong(0L));
                }
                return counts;
            });
        }
    }

    /**
     * Evolución temporal: noticias del usuario agrupadas por día (createdAt si
     * existe, si no publishedAt), separadas por nivel de riesgo. Últimos 14 días
     * con datos, devueltos en orden ascendente.
     */
    public List<NewsTimelineDto> newsTimeline(String userId) {
        // Agrupamos por la parte de fecha (YYYY-MM-DD) tomada del texto del valor
        // temporal. substring(toString(...)) es robusto tanto si createdAt/publishedAt
        // son datetime de Neo4j como si fueran strings ISO; evita que date(ts) falle.
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News)
                WITH n, coalesce(n.createdAt, n.publishedAt) AS ts
                WHERE ts IS NOT NULL
                WITH substring(toString(ts), 0, 10) AS date, n
                RETURN date,
                       sum(CASE WHEN n.riskLevel = 'LOW'    THEN 1 ELSE 0 END) AS low,
                       sum(CASE WHEN n.riskLevel = 'MEDIUM' THEN 1 ELSE 0 END) AS medium,
                       sum(CASE WHEN n.riskLevel = 'HIGH'   THEN 1 ELSE 0 END) AS high,
                       count(n) AS total
                ORDER BY date DESC
                LIMIT 14
                """;

        try (Session session = driver.session(sessionConfig)) {
            List<NewsTimelineDto> rows = session.executeRead(tx ->
                    tx.run(cypher, Map.of("userId", userId)).list(r -> new NewsTimelineDto(
                            r.get("date").asString(null),
                            r.get("low").asLong(0L),
                            r.get("medium").asLong(0L),
                            r.get("high").asLong(0L),
                            r.get("total").asLong(0L)
                    )));
            // La query trae los más recientes; los invertimos a orden ascendente.
            List<NewsTimelineDto> ascending = new ArrayList<>(rows);
            java.util.Collections.reverse(ascending);
            return ascending;
        }
    }

    /**
     * Resumen del subgrafo del usuario: conteo por tipo de nodo y total de
     * relaciones internas entre los nodos alcanzables desde sus noticias.
     */
    public GraphSummaryDto graphSummary(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(n:News) RETURN count(DISTINCT n) AS news }
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:PUBLISHED_BY]->(s:Source) RETURN count(DISTINCT s) AS sources }
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:ABOUT]->(t:Topic) RETURN count(DISTINCT t) AS topics }
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:CONTAINS]->(c:Claim) RETURN count(DISTINCT c) AS claims }
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:CONTAINS]->(:Claim)-[:SUPPORTED_BY|REFUTED_BY]->(e:Evidence) RETURN count(DISTINCT e) AS evidence }
                CALL (owner) { OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(:News)-[:CONTAINS]->(:Claim)<-[:CHECKS]-(fc:FactCheck) RETURN count(DISTINCT fc) AS factChecks }
                CALL (owner) { OPTIONAL MATCH (p:Post)-[:SPREADS]->(:News)<-[:OWNS_NEWS]-(owner) RETURN count(DISTINCT p) AS posts }
                CALL (owner) { OPTIONAL MATCH (u:User)-[:CREATED|SHARED]->(:Post)-[:SPREADS]->(:News)<-[:OWNS_NEWS]-(owner) RETURN count(DISTINCT u) AS users }
                CALL (owner) {
                  OPTIONAL MATCH (owner)-[:OWNS_NEWS]->(n:News)
                  OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
                  OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                  OPTIONAL MATCH (n)-[:CONTAINS]->(c:Claim)
                  OPTIONAL MATCH (c)-[:SUPPORTED_BY|REFUTED_BY]->(e:Evidence)
                  OPTIONAL MATCH (c)<-[:CHECKS]-(fc:FactCheck)
                  OPTIONAL MATCH (fc)-[:BASED_ON]->(e2:Evidence)
                  OPTIONAL MATCH (p:Post)-[:SPREADS]->(n)
                  OPTIONAL MATCH (u:User)-[:CREATED|SHARED]->(p)
                  WITH collect(DISTINCT n) + collect(DISTINCT s) + collect(DISTINCT t) +
                       collect(DISTINCT c) + collect(DISTINCT e) + collect(DISTINCT e2) +
                       collect(DISTINCT fc) + collect(DISTINCT p) + collect(DISTINCT u) AS raw
                  UNWIND raw AS node
                  WITH node WHERE node IS NOT NULL
                  WITH collect(DISTINCT node) AS nodeSet
                  UNWIND nodeSet AS a
                  OPTIONAL MATCH (a)-[rel]->(b)
                  WHERE b IN nodeSet
                  RETURN count(DISTINCT rel) AS totalRelationships
                }
                RETURN news, sources, topics, claims, evidence, factChecks, posts, users, totalRelationships
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Record r = tx.run(cypher, Map.of("userId", userId)).single();
                List<GraphSummaryNodeDto> nodes = List.of(
                        new GraphSummaryNodeDto("Noticias", r.get("news").asLong(0L), "News"),
                        new GraphSummaryNodeDto("Fuentes", r.get("sources").asLong(0L), "Source"),
                        new GraphSummaryNodeDto("Temas", r.get("topics").asLong(0L), "Topic"),
                        new GraphSummaryNodeDto("Claims", r.get("claims").asLong(0L), "Claim"),
                        new GraphSummaryNodeDto("Evidencias", r.get("evidence").asLong(0L), "Evidence"),
                        new GraphSummaryNodeDto("Fact Checks", r.get("factChecks").asLong(0L), "FactCheck"),
                        new GraphSummaryNodeDto("Posts", r.get("posts").asLong(0L), "Post"),
                        new GraphSummaryNodeDto("Usuarios", r.get("users").asLong(0L), "User")
                );
                return new GraphSummaryDto(nodes, r.get("totalRelationships").asLong(0L));
            });
        }
    }
}
