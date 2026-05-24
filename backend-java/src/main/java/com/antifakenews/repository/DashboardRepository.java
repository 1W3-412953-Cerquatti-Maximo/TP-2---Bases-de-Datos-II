package com.antifakenews.repository;

import com.antifakenews.dto.DashboardSummaryDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public DashboardRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public DashboardSummaryDto getSummary() {
        final String cypher = """
                CALL {
                  MATCH (n:News)
                  RETURN count(n) AS totalNews,
                         sum(CASE WHEN n.riskLevel = 'HIGH'   THEN 1 ELSE 0 END) AS highRiskNews,
                         sum(CASE WHEN n.riskLevel = 'MEDIUM' THEN 1 ELSE 0 END) AS mediumRiskNews,
                         sum(CASE WHEN n.riskLevel = 'LOW'    THEN 1 ELSE 0 END) AS lowRiskNews
                }
                CALL { MATCH (s:Source)    RETURN count(s) AS totalSources }
                CALL { MATCH (c:Claim)     RETURN count(c) AS totalClaims }
                CALL { MATCH (f:FactCheck) RETURN count(f) AS totalFactChecks }
                CALL { MATCH (p:Post)      RETURN count(p) AS totalPosts }
                CALL { MATCH (u:User)      RETURN count(u) AS totalUsers }
                RETURN totalNews, highRiskNews, mediumRiskNews, lowRiskNews,
                       totalSources, totalClaims, totalFactChecks, totalPosts, totalUsers
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Record r = tx.run(cypher).single();
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
