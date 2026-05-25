package com.antifakenews.repository;

import com.antifakenews.dto.DashboardSummaryDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

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
                  RETURN count(DISTINCT s) AS totalSources
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
                       totalSources, totalClaims, totalFactChecks, totalPosts, totalUsers
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
                        r.get("totalClaims").asLong(0L),
                        r.get("totalFactChecks").asLong(0L),
                        r.get("totalPosts").asLong(0L),
                        r.get("totalUsers").asLong(0L)
                );
            });
        }
    }
}
