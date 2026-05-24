package com.antifakenews.controller;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public HealthController(Driver driver, SessionConfig sessionConfig) {
        this.driver = driver;
        this.sessionConfig = sessionConfig;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @GetMapping("/neo4j")
    public ResponseEntity<Map<String, Object>> neo4jHealth() {
        try (Session session = driver.session(sessionConfig)) {
            Result result = session.run("RETURN 1 AS result");
            Record record = result.single();
            long value = record.get("result").asLong();
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "result", value
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage() == null ? "Unknown error" : e.getMessage()
            ));
        }
    }
}
