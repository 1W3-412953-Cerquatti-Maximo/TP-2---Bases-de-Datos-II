package com.antifakenews.repository;

import com.antifakenews.dto.ClaimDto;
import com.antifakenews.dto.EvidenceDto;
import com.antifakenews.dto.FactCheckDto;
import com.antifakenews.dto.NewsDetailDto;
import com.antifakenews.dto.NewsSummaryDto;
import com.antifakenews.dto.PostDto;
import com.antifakenews.dto.SourceDto;
import com.antifakenews.dto.TopicDto;
import com.antifakenews.dto.UserDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NewsRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public NewsRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public List<NewsSummaryDto> findAll() {
        final String cypher = """
                MATCH (n:News)
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
                OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                RETURN n.id          AS id,
                       n.title       AS title,
                       n.url         AS url,
                       n.publishedAt AS publishedAt,
                       n.status      AS status,
                       n.riskScore   AS riskScore,
                       n.riskLevel   AS riskLevel,
                       s.name        AS sourceName,
                       collect(DISTINCT t.name) AS topicNames
                ORDER BY n.publishedAt DESC
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(cypher).list(r -> new NewsSummaryDto(
                    r.get("id").asString(null),
                    r.get("title").asString(null),
                    r.get("url").asString(null),
                    r.get("publishedAt").isNull() ? null : r.get("publishedAt").asZonedDateTime(),
                    r.get("status").asString(null),
                    r.get("riskScore").isNull() ? null : r.get("riskScore").asLong(),
                    r.get("riskLevel").asString(null),
                    r.get("sourceName").asString(null),
                    r.get("topicNames").asList(Value::asString)
            )));
        }
    }

    public Optional<NewsDetailDto> findById(String id) {
        final String cypher = """
                MATCH (n:News {id: $id})
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(src:Source)
                CALL (n) {
                  OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                  RETURN collect(DISTINCT t {.id, .name, .slug}) AS topics
                }
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(c:Claim)
                  RETURN collect(DISTINCT c {.id, .text, .status}) AS claims
                }
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(:Claim)-[:SUPPORTED_BY|REFUTED_BY]->(e:Evidence)
                  RETURN collect(DISTINCT e {.id, .description, .type, .url}) AS evidence
                }
                CALL (n) {
                  OPTIONAL MATCH (n)-[:CONTAINS]->(:Claim)<-[:CHECKS]-(fc:FactCheck)
                  RETURN collect(DISTINCT fc {.id, .verdict, .explanation, .confidence, .publishedAt}) AS factChecks
                }
                CALL (n) {
                  OPTIONAL MATCH (p:Post)-[:SPREADS]->(n)
                  RETURN collect(DISTINCT p {.id, .content, .platform, .createdAt}) AS posts
                }
                CALL (n) {
                  OPTIONAL MATCH (u:User)-[:CREATED|SHARED]->(:Post)-[:SPREADS]->(n)
                  RETURN collect(DISTINCT u {.id, .username, .role}) AS users
                }
                RETURN n.id          AS id,
                       n.title       AS title,
                       n.content     AS content,
                       n.url         AS url,
                       n.publishedAt AS publishedAt,
                       n.status      AS status,
                       n.riskScore   AS riskScore,
                       n.riskLevel   AS riskLevel,
                       src.id        AS sourceId,
                       src.name      AS sourceName,
                       src.type      AS sourceType,
                       src.credibilityScore AS sourceCredibility,
                       src.url       AS sourceUrl,
                       topics, claims, evidence, factChecks, posts, users
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("id", id));
                if (!result.hasNext()) {
                    return Optional.<NewsDetailDto>empty();
                }
                Record r = result.next();

                SourceDto source = r.get("sourceId").isNull() ? null : new SourceDto(
                        r.get("sourceId").asString(null),
                        r.get("sourceName").asString(null),
                        r.get("sourceType").asString(null),
                        r.get("sourceCredibility").isNull() ? null : r.get("sourceCredibility").asDouble(),
                        r.get("sourceUrl").asString(null)
                );

                List<TopicDto> topics = r.get("topics").asList(v -> new TopicDto(
                        v.get("id").asString(null),
                        v.get("name").asString(null),
                        v.get("slug").asString(null)
                ));

                List<ClaimDto> claims = r.get("claims").asList(v -> new ClaimDto(
                        v.get("id").asString(null),
                        v.get("text").asString(null),
                        v.get("status").asString(null)
                ));

                List<EvidenceDto> evidence = r.get("evidence").asList(v -> new EvidenceDto(
                        v.get("id").asString(null),
                        v.get("description").asString(null),
                        v.get("type").asString(null),
                        v.get("url").asString(null)
                ));

                List<FactCheckDto> factChecks = r.get("factChecks").asList(v -> new FactCheckDto(
                        v.get("id").asString(null),
                        v.get("verdict").asString(null),
                        v.get("explanation").asString(null),
                        v.get("confidence").isNull() ? null : v.get("confidence").asDouble(),
                        v.get("publishedAt").isNull() ? null : v.get("publishedAt").asZonedDateTime()
                ));

                List<PostDto> posts = r.get("posts").asList(v -> new PostDto(
                        v.get("id").asString(null),
                        v.get("content").asString(null),
                        v.get("platform").asString(null),
                        v.get("createdAt").isNull() ? null : v.get("createdAt").asZonedDateTime()
                ));

                List<UserDto> users = r.get("users").asList(v -> new UserDto(
                        v.get("id").asString(null),
                        v.get("username").asString(null),
                        v.get("role").asString(null)
                ));

                return Optional.of(new NewsDetailDto(
                        r.get("id").asString(null),
                        r.get("title").asString(null),
                        r.get("content").asString(null),
                        r.get("url").asString(null),
                        r.get("publishedAt").isNull() ? null : r.get("publishedAt").asZonedDateTime(),
                        r.get("status").asString(null),
                        r.get("riskScore").isNull() ? null : r.get("riskScore").asLong(),
                        r.get("riskLevel").asString(null),
                        source, topics, claims, evidence, factChecks, posts, users
                ));
            });
        }
    }
}
