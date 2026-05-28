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

    public List<NewsSummaryDto> findAll(String userId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News)
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
                OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                RETURN n.id          AS id,
                       n.title       AS title,
                       n.url         AS url,
                       n.publishedAt AS publishedAt,
                       n.createdAt   AS createdAt,
                       n.status      AS status,
                       n.riskScore   AS riskScore,
                       n.riskLevel   AS riskLevel,
                       s.name        AS sourceName,
                       collect(DISTINCT t.name) AS topicNames
                ORDER BY coalesce(n.publishedAt, n.createdAt) DESC
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("userId", userId)).list(r -> new NewsSummaryDto(
                    r.get("id").asString(null),
                    r.get("title").asString(null),
                    r.get("url").asString(null),
                    r.get("publishedAt").isNull() ? null : r.get("publishedAt").asZonedDateTime(),
                    r.get("createdAt").isNull() ? null : r.get("createdAt").asZonedDateTime(),
                    r.get("status").asString(null),
                    r.get("riskScore").isNull() ? null : r.get("riskScore").asLong(),
                    r.get("riskLevel").asString(null),
                    r.get("sourceName").asString(null),
                    r.get("topicNames").asList(Value::asString)
            )));
        }
    }

    /**
     * Crea una noticia enviada por URL y la asocia al usuario autenticado.
     * Reutiliza la Source por dominio y los Topic por nombre (MERGE). Persiste el
     * riskScore/riskLevel/status recibidos (ya validados en el servicio). Cada tema
     * llega como {name, source} y se vincula con (:News)-[:ABOUT {relevance, source}].
     */
    public NewsCreationResult createUserSubmittedNews(
            String userId,
            String newsId, String title, String content, String url,
            String sourceId, String sourceName, String domain,
            long riskScore, String riskLevel, String status,
            String publishedAtIso, String publishedAtSource, Double publishedAtConfidence,
            List<Map<String, Object>> topics) {

        final String cypher = """
                MATCH (owner:AppUser {id: $userId})
                CREATE (n:News {
                  id: $newsId,
                  title: $title,
                  content: $content,
                  url: $url,
                  createdAt: datetime(),
                  publishedAt: CASE WHEN $publishedAtIso IS NULL THEN null ELSE datetime($publishedAtIso) END,
                  publishedAtSource: $publishedAtSource,
                  publishedAtConfidence: $publishedAtConfidence,
                  status: $status,
                  riskScore: $riskScore,
                  riskLevel: $riskLevel,
                  origin: 'USER_SUBMITTED_URL'
                })
                CREATE (owner)-[:OWNS_NEWS {createdAt: datetime(), origin: 'USER_SUBMISSION'}]->(n)
                MERGE (s:Source {domain: $domain})
                  ON CREATE SET s.id = $sourceId,
                                s.name = $sourceName,
                                s.type = 'WEB',
                                s.url = $url,
                                s.credibilityScore = 0.5,
                                s.country = 'UNKNOWN'
                CREATE (n)-[:PUBLISHED_BY {firstSeenAt: datetime(), sourceUrl: $url}]->(s)
                FOREACH (topic IN $topics |
                  MERGE (t:Topic {name: topic.name})
                    ON CREATE SET t.id = randomUUID(),
                                  t.slug = toLower(replace(topic.name, ' ', '-'))
                  CREATE (n)-[:ABOUT {relevance: 0.5, source: topic.source}]->(t)
                )
                WITH n, s
                OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                RETURN n.id        AS newsId,
                       n.title     AS title,
                       n.url       AS url,
                       n.status    AS status,
                       n.riskScore AS riskScore,
                       n.riskLevel AS riskLevel,
                       s.name      AS sourceName,
                       collect(DISTINCT t.name) AS topicNames
                """;

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", userId);
        params.put("newsId", newsId);
        params.put("title", title);
        params.put("content", content);
        params.put("url", url);
        params.put("sourceId", sourceId);
        params.put("sourceName", sourceName);
        params.put("domain", domain);
        params.put("publishedAtIso", publishedAtIso);
        params.put("publishedAtSource", publishedAtSource);
        params.put("publishedAtConfidence", publishedAtConfidence);
        params.put("riskScore", riskScore);
        params.put("riskLevel", riskLevel);
        params.put("status", status);
        params.put("topics", topics);

        try (Session session = driver.session(sessionConfig)) {
            return session.executeWrite(tx -> {
                Record r = tx.run(cypher, params).single();
                return new NewsCreationResult(
                        r.get("newsId").asString(null),
                        r.get("title").asString(null),
                        r.get("url").asString(null),
                        r.get("status").asString(null),
                        r.get("riskScore").asLong(0L),
                        r.get("riskLevel").asString(null),
                        r.get("sourceName").asString(null),
                        r.get("topicNames").asList(Value::asString)
                );
            });
        }
    }

    /**
     * Elimina una noticia que pertenezca al usuario autenticado.
     * Borra la News (DETACH DELETE quita OWNS_NEWS, PUBLISHED_BY, ABOUT, CONTAINS…),
     * los Post que difunden exclusivamente esta noticia, y la Source solo si queda
     * sin ninguna otra News asociada globalmente. No toca AppUser, Topic, Claim,
     * Evidence ni FactCheck (pueden ser compartidos). Empty => 404.
     */
    public Optional<DeleteResult> deleteOwnedNews(String userId, String newsId) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $newsId})
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
                OPTIONAL MATCH (s)<-[:PUBLISHED_BY]-(otherNews:News)
                  WHERE otherNews <> n
                WITH n, s, count(DISTINCT otherNews) AS otherNewsCount
                OPTIONAL MATCH (p:Post)-[:SPREADS]->(n)
                  WHERE NOT EXISTS { MATCH (p)-[:SPREADS]->(other:News) WHERE other <> n }
                WITH n, s, otherNewsCount, collect(DISTINCT p) AS exclusivePosts
                WITH n, s, exclusivePosts,
                     (s IS NOT NULL AND otherNewsCount = 0) AS deleteSource,
                     s.name AS sourceName
                FOREACH (post IN exclusivePosts | DETACH DELETE post)
                DETACH DELETE n
                FOREACH (src IN CASE WHEN deleteSource THEN [s] ELSE [] END | DETACH DELETE src)
                RETURN $newsId AS newsId, true AS deleted, deleteSource AS sourceDeleted, sourceName AS sourceName
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("userId", userId, "newsId", newsId));
                if (!result.hasNext()) {
                    return Optional.<DeleteResult>empty();
                }
                Record r = result.next();
                return Optional.of(new DeleteResult(
                        r.get("newsId").asString(null),
                        r.get("deleted").asBoolean(false),
                        r.get("sourceDeleted").asBoolean(false),
                        r.get("sourceName").asString(null)
                ));
            });
        }
    }

    /**
     * Busca una noticia ya existente del usuario por URL exacta (la URL se compara
     * normalizada por el servicio antes de llegar acá). Se usa para evitar duplicar
     * la misma noticia al re-enviar la URL desde "Evaluar y guardar".
     */
    public Optional<ExistingNewsByUrl> findIdByUserAndUrl(String userId, String url) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {url: $url})
                OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
                OPTIONAL MATCH (n)-[:ABOUT]->(t:Topic)
                RETURN n.id        AS newsId,
                       n.title     AS title,
                       n.url       AS url,
                       n.status    AS status,
                       n.riskScore AS riskScore,
                       n.riskLevel AS riskLevel,
                       s.name      AS sourceName,
                       collect(DISTINCT t.name) AS topicNames
                LIMIT 1
                """;

        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("userId", userId, "url", url));
                if (!result.hasNext()) {
                    return Optional.<ExistingNewsByUrl>empty();
                }
                Record r = result.next();
                return Optional.of(new ExistingNewsByUrl(
                        r.get("newsId").asString(null),
                        r.get("title").asString(null),
                        r.get("url").asString(null),
                        r.get("status").asString(null),
                        r.get("riskScore").isNull() ? 0L : r.get("riskScore").asLong(),
                        r.get("riskLevel").asString(null),
                        r.get("sourceName").asString(null),
                        r.get("topicNames").asList(Value::asString)
                ));
            });
        }
    }

    public Optional<NewsDetailDto> findById(String userId, String id) {
        final String cypher = """
                MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News {id: $id})
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
                  OPTIONAL MATCH (n)-[:CONTAINS]->(:Claim)-[:SUPPORTED_BY|REFUTED_BY|HAS_EVIDENCE_GAP]->(e:Evidence)
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
                       n.createdAt   AS createdAt,
                       n.publishedAtSource     AS publishedAtSource,
                       n.publishedAtConfidence AS publishedAtConfidence,
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
                Result result = tx.run(cypher, Map.of("userId", userId, "id", id));
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
                        r.get("createdAt").isNull() ? null : r.get("createdAt").asZonedDateTime(),
                        r.get("publishedAtSource").asString(null),
                        r.get("publishedAtConfidence").isNull() ? null : r.get("publishedAtConfidence").asDouble(),
                        r.get("status").asString(null),
                        r.get("riskScore").isNull() ? null : r.get("riskScore").asLong(),
                        r.get("riskLevel").asString(null),
                        source, topics, claims, evidence, factChecks, posts, users
                ));
            });
        }
    }

    /** Proyección del resultado de crear una noticia por URL. */
    public record NewsCreationResult(
            String newsId,
            String title,
            String url,
            String status,
            long riskScore,
            String riskLevel,
            String sourceName,
            List<String> topicNames
    ) {}

    /** Proyección del resultado de eliminar una noticia. */
    public record DeleteResult(
            String newsId,
            boolean deleted,
            boolean sourceDeleted,
            String sourceName
    ) {}

    /** Proyección mínima para detectar una News ya guardada del usuario por URL. */
    public record ExistingNewsByUrl(
            String newsId,
            String title,
            String url,
            String status,
            long riskScore,
            String riskLevel,
            String sourceName,
            List<String> topicNames
    ) {}
}
