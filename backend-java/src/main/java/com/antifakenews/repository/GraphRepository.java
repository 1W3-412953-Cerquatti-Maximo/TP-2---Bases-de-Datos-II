package com.antifakenews.repository;

import com.antifakenews.dto.GraphEdgeDto;
import com.antifakenews.dto.GraphNodeDto;
import com.antifakenews.dto.GraphResponseDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GraphRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public GraphRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public Optional<GraphResponseDto> getNewsGraph(String userId, String id) {
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                String newsTitle = fetchNewsTitle(tx, userId, id);
                if (newsTitle == null) {
                    return Optional.<GraphResponseDto>empty();
                }

                Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
                List<GraphEdgeDto> edges = new ArrayList<>();

                nodes.put(id, new GraphNodeDto(id, "News", newsTitle));

                addSource(tx, id, nodes, edges);
                addTopics(tx, id, nodes, edges);
                addClaimsEvidenceAndFactChecks(tx, id, nodes, edges);
                addPostsAndUsers(tx, id, nodes, edges);

                return Optional.of(new GraphResponseDto(new ArrayList<>(nodes.values()), edges));
            });
        }
    }

    private String fetchNewsTitle(org.neo4j.driver.TransactionContext tx, String userId, String id) {
        // La pertenencia se valida acá: si la noticia no es del usuario, no hay título y el grafo es 404.
        Result r = tx.run(
                "MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $id}) RETURN n.title AS title",
                Map.of("userId", userId, "id", id));
        if (!r.hasNext()) return null;
        return r.next().get("title").asString(null);
    }

    private void addSource(org.neo4j.driver.TransactionContext tx, String newsId,
                           Map<String, GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
        final String cypher = """
                MATCH (n:News {id: $id})-[r:PUBLISHED_BY]->(s:Source)
                RETURN s.id AS id, s.name AS name, properties(r) AS relProps
                """;
        for (Record rec : tx.run(cypher, Map.of("id", newsId)).list()) {
            String sourceId = rec.get("id").asString(null);
            nodes.putIfAbsent(sourceId, new GraphNodeDto(sourceId, "Source", rec.get("name").asString(null)));
            edges.add(new GraphEdgeDto(newsId, sourceId, "PUBLISHED_BY", rec.get("relProps").asMap()));
        }
    }

    private void addTopics(org.neo4j.driver.TransactionContext tx, String newsId,
                           Map<String, GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
        final String cypher = """
                MATCH (n:News {id: $id})-[r:ABOUT]->(t:Topic)
                RETURN t.id AS id, t.name AS name, properties(r) AS relProps
                """;
        for (Record rec : tx.run(cypher, Map.of("id", newsId)).list()) {
            String topicId = rec.get("id").asString(null);
            nodes.putIfAbsent(topicId, new GraphNodeDto(topicId, "Topic", rec.get("name").asString(null)));
            edges.add(new GraphEdgeDto(newsId, topicId, "ABOUT", rec.get("relProps").asMap()));
        }
    }

    private void addClaimsEvidenceAndFactChecks(org.neo4j.driver.TransactionContext tx, String newsId,
                                                Map<String, GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
        // Claims via CONTAINS
        final String claimsCypher = """
                MATCH (n:News {id: $id})-[r:CONTAINS]->(c:Claim)
                RETURN c.id AS id, c.text AS text, properties(r) AS relProps
                """;
        for (Record rec : tx.run(claimsCypher, Map.of("id", newsId)).list()) {
            String claimId = rec.get("id").asString(null);
            nodes.putIfAbsent(claimId, new GraphNodeDto(claimId, "Claim", rec.get("text").asString(null)));
            edges.add(new GraphEdgeDto(newsId, claimId, "CONTAINS", rec.get("relProps").asMap()));
        }

        // Evidence linked to those claims via SUPPORTED_BY / REFUTED_BY
        final String evidenceCypher = """
                MATCH (n:News {id: $id})-[:CONTAINS]->(c:Claim)-[r:SUPPORTED_BY|REFUTED_BY]->(e:Evidence)
                RETURN c.id AS claimId, e.id AS evidenceId, e.description AS description,
                       type(r) AS relType, properties(r) AS relProps
                """;
        for (Record rec : tx.run(evidenceCypher, Map.of("id", newsId)).list()) {
            String claimId = rec.get("claimId").asString(null);
            String evidenceId = rec.get("evidenceId").asString(null);
            nodes.putIfAbsent(evidenceId, new GraphNodeDto(evidenceId, "Evidence", rec.get("description").asString(null)));
            edges.add(new GraphEdgeDto(claimId, evidenceId, rec.get("relType").asString(null), rec.get("relProps").asMap()));
        }

        // FactChecks that CHECK the claims
        final String checksCypher = """
                MATCH (n:News {id: $id})-[:CONTAINS]->(c:Claim)<-[r:CHECKS]-(fc:FactCheck)
                RETURN c.id AS claimId, fc.id AS factCheckId, fc.verdict AS verdict, properties(r) AS relProps
                """;
        for (Record rec : tx.run(checksCypher, Map.of("id", newsId)).list()) {
            String factCheckId = rec.get("factCheckId").asString(null);
            String claimId = rec.get("claimId").asString(null);
            nodes.putIfAbsent(factCheckId, new GraphNodeDto(factCheckId, "FactCheck", rec.get("verdict").asString(null)));
            edges.add(new GraphEdgeDto(factCheckId, claimId, "CHECKS", rec.get("relProps").asMap()));
        }

        // Evidence used by those FactChecks via BASED_ON
        final String basedOnCypher = """
                MATCH (n:News {id: $id})-[:CONTAINS]->(:Claim)<-[:CHECKS]-(fc:FactCheck)-[r:BASED_ON]->(e:Evidence)
                RETURN fc.id AS factCheckId, e.id AS evidenceId, e.description AS description, properties(r) AS relProps
                """;
        for (Record rec : tx.run(basedOnCypher, Map.of("id", newsId)).list()) {
            String factCheckId = rec.get("factCheckId").asString(null);
            String evidenceId = rec.get("evidenceId").asString(null);
            nodes.putIfAbsent(evidenceId, new GraphNodeDto(evidenceId, "Evidence", rec.get("description").asString(null)));
            edges.add(new GraphEdgeDto(factCheckId, evidenceId, "BASED_ON", rec.get("relProps").asMap()));
        }
    }

    private void addPostsAndUsers(org.neo4j.driver.TransactionContext tx, String newsId,
                                  Map<String, GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
        // Posts that spread the news
        final String postsCypher = """
                MATCH (p:Post)-[r:SPREADS]->(n:News {id: $id})
                RETURN p.id AS id, p.content AS content, properties(r) AS relProps
                """;
        for (Record rec : tx.run(postsCypher, Map.of("id", newsId)).list()) {
            String postId = rec.get("id").asString(null);
            nodes.putIfAbsent(postId, new GraphNodeDto(postId, "Post", rec.get("content").asString(null)));
            edges.add(new GraphEdgeDto(postId, newsId, "SPREADS", rec.get("relProps").asMap()));
        }

        // Users that CREATED or SHARED those posts
        final String usersCypher = """
                MATCH (u:User)-[r:CREATED|SHARED]->(p:Post)-[:SPREADS]->(n:News {id: $id})
                RETURN u.id AS userId, u.username AS username,
                       p.id AS postId, type(r) AS relType, properties(r) AS relProps
                """;
        for (Record rec : tx.run(usersCypher, Map.of("id", newsId)).list()) {
            String userId = rec.get("userId").asString(null);
            String postId = rec.get("postId").asString(null);
            nodes.putIfAbsent(userId, new GraphNodeDto(userId, "User", rec.get("username").asString(null)));
            edges.add(new GraphEdgeDto(userId, postId, rec.get("relType").asString(null), rec.get("relProps").asMap()));
        }
    }
}
