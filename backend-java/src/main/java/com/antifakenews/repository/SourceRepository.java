package com.antifakenews.repository;

import com.antifakenews.dto.SourceDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SourceRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public SourceRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public List<SourceDto> findAll(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(:News)-[:PUBLISHED_BY]->(s:Source)
                WITH DISTINCT s
                RETURN s.id                 AS id,
                       s.name               AS name,
                       s.type               AS type,
                       s.credibilityScore   AS credibilityScore,
                       s.url                AS url
                ORDER BY s.credibilityScore DESC, s.name ASC
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("userId", userId)).list(r -> new SourceDto(
                    r.get("id").asString(null),
                    r.get("name").asString(null),
                    r.get("type").asString(null),
                    r.get("credibilityScore").isNull() ? null : r.get("credibilityScore").asDouble(),
                    r.get("url").asString(null)
            )));
        }
    }
}
