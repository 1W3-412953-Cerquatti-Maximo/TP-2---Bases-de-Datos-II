package com.antifakenews.repository;

import com.antifakenews.dto.CurrentUserDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class AuthRepository {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public AuthRepository(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    public boolean existsByEmail(String email) {
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx
                    .run("MATCH (u:AppUser {email: $email}) RETURN count(u) > 0 AS exists",
                            Map.of("email", email))
                    .single().get("exists").asBoolean());
        }
    }

    public boolean existsByUsername(String username) {
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> tx
                    .run("MATCH (u:AppUser {username: $username}) RETURN count(u) > 0 AS exists",
                            Map.of("username", username))
                    .single().get("exists").asBoolean());
        }
    }

    public CurrentUserDto createUser(String id, String username, String email, String displayName,
                                     String passwordHash, String role, String themePreference) {
        final String cypher = """
                CREATE (u:AppUser {
                  id: $id,
                  username: $username,
                  email: $email,
                  displayName: $displayName,
                  passwordHash: $passwordHash,
                  role: $role,
                  themePreference: $themePreference,
                  createdAt: datetime()
                })
                RETURN u.id AS id, u.username AS username, u.email AS email,
                       u.displayName AS displayName, u.role AS role,
                       u.themePreference AS themePreference
                """;
        Map<String, Object> params = Map.of(
                "id", id, "username", username, "email", email, "displayName", displayName,
                "passwordHash", passwordHash, "role", role, "themePreference", themePreference);

        try (Session session = driver.session(sessionConfig)) {
            return session.executeWrite(tx -> toCurrentUser(tx.run(cypher, params).single()));
        }
    }

    public Optional<StoredUser> findByEmail(String email) {
        final String cypher = """
                MATCH (u:AppUser {email: $email})
                RETURN u.id AS id, u.username AS username, u.email AS email,
                       u.displayName AS displayName, u.role AS role,
                       u.themePreference AS themePreference, u.passwordHash AS passwordHash
                """;
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("email", email));
                if (!result.hasNext()) {
                    return Optional.<StoredUser>empty();
                }
                Record r = result.next();
                return Optional.of(new StoredUser(
                        r.get("id").asString(null),
                        r.get("username").asString(null),
                        r.get("email").asString(null),
                        r.get("displayName").asString(null),
                        r.get("role").asString(null),
                        r.get("themePreference").asString(null),
                        r.get("passwordHash").asString(null)
                ));
            });
        }
    }

    public void createSession(String userId, String token, long ttlDays) {
        final String cypher = """
                MATCH (u:AppUser {id: $userId})
                CREATE (u)-[:HAS_SESSION]->(s:AuthSession {
                  token: $token,
                  createdAt: datetime(),
                  expiresAt: datetime() + duration({days: $ttlDays})
                })
                """;
        try (Session session = driver.session(sessionConfig)) {
            session.executeWrite(tx -> {
                tx.run(cypher, Map.of("userId", userId, "token", token, "ttlDays", ttlDays));
                return null;
            });
        }
    }

    public Optional<CurrentUserDto> findUserByToken(String token) {
        final String cypher = """
                MATCH (u:AppUser)-[:HAS_SESSION]->(s:AuthSession {token: $token})
                WHERE s.expiresAt > datetime()
                RETURN u.id AS id, u.username AS username, u.email AS email,
                       u.displayName AS displayName, u.role AS role,
                       u.themePreference AS themePreference
                """;
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("token", token));
                if (!result.hasNext()) {
                    return Optional.<CurrentUserDto>empty();
                }
                return Optional.of(toCurrentUser(result.next()));
            });
        }
    }

    public Optional<CurrentUserDto> updateThemePreference(String userId, String themePreference) {
        final String cypher = """
                MATCH (u:AppUser {id: $userId})
                SET u.themePreference = $themePreference
                RETURN u.id AS id, u.username AS username, u.email AS email,
                       u.displayName AS displayName, u.role AS role,
                       u.themePreference AS themePreference
                """;
        try (Session session = driver.session(sessionConfig)) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("userId", userId, "themePreference", themePreference));
                if (!result.hasNext()) {
                    return Optional.<CurrentUserDto>empty();
                }
                return Optional.of(toCurrentUser(result.next()));
            });
        }
    }

    public void deleteSession(String token) {
        final String cypher = """
                MATCH (:AppUser)-[:HAS_SESSION]->(s:AuthSession {token: $token})
                DETACH DELETE s
                """;
        try (Session session = driver.session(sessionConfig)) {
            session.executeWrite(tx -> {
                tx.run(cypher, Map.of("token", token));
                return null;
            });
        }
    }

    private static CurrentUserDto toCurrentUser(Record r) {
        return new CurrentUserDto(
                r.get("id").asString(null),
                r.get("username").asString(null),
                r.get("email").asString(null),
                r.get("displayName").asString(null),
                r.get("role").asString(null),
                r.get("themePreference").asString(null)
        );
    }

    /** Proyección interna que incluye el hash — nunca se expone al cliente. */
    public record StoredUser(
            String id,
            String username,
            String email,
            String displayName,
            String role,
            String themePreference,
            String passwordHash
    ) {}
}
