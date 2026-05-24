package com.antifakenews.repository;

import com.antifakenews.dto.TopicDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TopicRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public TopicRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public List<TopicDto> findAll() {
        final String cypher = """
                MATCH (t:Topic)
                RETURN t.id AS id, t.name AS name, t.slug AS slug
                ORDER BY t.name ASC
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(cypher).list(r -> new TopicDto(
                    r.get("id").asString(null),
                    r.get("name").asString(null),
                    r.get("slug").asString(null)
            )));
        }
    }
}
