package com.antifakenews.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Value("${neo4j.uri}")
    private String uri;

    @Value("${neo4j.user}")
    private String user;

    @Value("${neo4j.password}")
    private String password;

    @Value("${neo4j.database}")
    private String database;

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Bean
    public SessionConfig sessionConfig() {
        return SessionConfig.forDatabase(database);
    }
}
