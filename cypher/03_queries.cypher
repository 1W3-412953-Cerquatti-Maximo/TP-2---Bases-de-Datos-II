// ============================================================
// 03_queries.cypher
// Consultas de demostración — NexoVeraz (App Anti Fake News)
// TP-2 Bases de Datos II
//
// Cada bloque es una consulta independiente, pensada para
// ejecutarse en Neo4j Query (Browser) sobre la base
// `antifakenews` ya cargada con los scripts 01 y 02.
//
// Las consultas están comentadas para servir como guion
// durante la defensa del trabajo: qué pregunta responde,
// qué patrón del grafo recorre y qué propiedad analiza.
// ============================================================


// ------------------------------------------------------------
// Q1. Listar noticias con su fuente y la confiabilidad asociada.
//
// Patrón:  (News)-[:PUBLISHED_BY]->(Source)
// Para qué: panorama editorial — "¿quién publica qué?".
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News)-[:PUBLISHED_BY]->(s:Source)
RETURN n.id           AS newsId,
       n.title        AS title,
       n.publishedAt  AS publishedAt,
       s.name         AS source,
       s.credibilityScore AS credibility
ORDER BY n.publishedAt DESC;


// ------------------------------------------------------------
// Q2. Noticias de alto riesgo (candidatas a fake news).
//
// Criterio: riskScore >= 70 (escala 0..100) o riskLevel = 'HIGH'.
// Para qué: alimentar un panel de alertas / cola de revisión.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News)
WHERE n.riskScore >= 70
RETURN n.id        AS id,
       n.title     AS title,
       n.riskScore AS riskScore,
       n.riskLevel AS riskLevel,
       n.url       AS url
ORDER BY n.riskScore DESC;


// ------------------------------------------------------------
// Q3. Noticias agrupadas por tema, con muestra de títulos.
//
// Patrón:  (News)-[:ABOUT]->(Topic)
// Agregación: count + collect.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News)-[:ABOUT]->(t:Topic)
RETURN t.name AS topic,
       count(n)                       AS newsCount,
       collect(n.title)[..3]          AS sampleTitles
ORDER BY newsCount DESC;


// ------------------------------------------------------------
// Q4. Fuentes con baja confiabilidad y cuántas noticias publicaron.
//
// Criterio: credibilityScore < 0.5.
// OPTIONAL MATCH para incluir fuentes aún sin noticias.
// ------------------------------------------------------------
CYPHER 25
MATCH (s:Source)
WHERE s.credibilityScore < 0.5
OPTIONAL MATCH (n:News)-[:PUBLISHED_BY]->(s)
RETURN s.id                  AS sourceId,
       s.name                AS source,
       s.credibilityScore    AS credibility,
       count(n)              AS newsPublished
ORDER BY s.credibilityScore ASC;


// ------------------------------------------------------------
// Q5. Usuarios que crearon o compartieron posts de noticias riesgosas.
//
// Patrón:  (User)-[:CREATED|SHARED]->(Post)-[:SPREADS]->(News)
// Criterio: riskScore >= 70 (escala 0..100).
// Métrica: cantidad de noticias riesgosas distintas alcanzadas
//          y rol del usuario (útil para priorizar revisión humana).
// ------------------------------------------------------------
CYPHER 25
MATCH (u:User)-[:CREATED|SHARED]->(p:Post)-[:SPREADS]->(n:News)
WHERE n.riskScore >= 70
RETURN u.username                       AS user,
       u.role                           AS role,
       u.followerCount                  AS followers,
       count(DISTINCT n)                AS riskyNewsReached,
       collect(DISTINCT n.title)        AS titles
ORDER BY riskyNewsReached DESC, followers DESC;


// ------------------------------------------------------------
// Q6. Claims refutados, con sus evidencias y el fact check asociado.
//
// Patrón:  (Claim)-[:REFUTED_BY]->(Evidence)  +  (FactCheck)-[:CHECKS]->(Claim)
// Para qué: lista canónica de "afirmaciones desmentidas".
// ------------------------------------------------------------
CYPHER 25
MATCH (c:Claim {status: 'REFUTED'})
OPTIONAL MATCH (c)-[:REFUTED_BY]->(e:Evidence)
OPTIONAL MATCH (fc:FactCheck)-[:CHECKS]->(c)
RETURN c.text                         AS claim,
       fc.verdict                     AS verdict,
       fc.confidence                  AS confidence,
       collect(DISTINCT e.description) AS evidences
ORDER BY confidence DESC;


// ------------------------------------------------------------
// Q7. Evidencias asociadas a una noticia puntual (ej. news-003).
//
// Patrón:  (News)-[:CONTAINS]->(Claim)-[:SUPPORTED_BY|REFUTED_BY]->(Evidence)
// El type(r) permite distinguir si la evidencia apoya o refuta
// el claim contenido en la noticia.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News {id: 'news-003'})-[:CONTAINS]->(c:Claim)-[r:SUPPORTED_BY|REFUTED_BY]->(e:Evidence)
RETURN n.title         AS news,
       c.text          AS claim,
       type(r)         AS relation,
       e.description   AS evidence,
       e.url           AS evidenceUrl;


// ------------------------------------------------------------
// Q8. Recorrido completo de verificación:
//     News -> Claim -> FactCheck -> Evidence
//
// Este es EL recorrido central del modelo: mostrar cómo una
// afirmación de una noticia llega a un veredicto sustentado.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News)-[:CONTAINS]->(c:Claim)<-[:CHECKS]-(fc:FactCheck)-[:BASED_ON]->(e:Evidence)
RETURN n.title                     AS news,
       c.text                      AS claim,
       fc.verdict                  AS verdict,
       fc.explanation              AS explanation,
       collect(DISTINCT e.description) AS evidenceUsed
ORDER BY fc.confidence DESC;


// ------------------------------------------------------------
// Q9. Propagación de una noticia: User -> Post -> News.
//
// Muestra los autores originales (CREATED) y los amplificadores
// (SHARED) de una noticia puntual.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News {id: 'news-003'})<-[:SPREADS]-(p:Post)
OPTIONAL MATCH (author:User)-[:CREATED]->(p)
OPTIONAL MATCH (sharer:User)-[:SHARED]->(p)
RETURN p.id              AS postId,
       p.platform        AS platform,
       p.createdAt       AS postedAt,
       author.username   AS createdBy,
       collect(DISTINCT sharer.username) AS sharedBy;


// ------------------------------------------------------------
// Q10. Usuarios conectados que difundieron la misma noticia.
//
// Patrón:  dos usuarios distintos que comparten/crean posts
//          que apuntan a la misma noticia, y además están
//          enlazados socialmente (FOLLOWS o INTERACTS_WITH).
//
// Para qué: detectar clusters de difusión coordinada.
// ------------------------------------------------------------
CYPHER 25
MATCH (u1:User)-[:CREATED|SHARED]->(p1:Post)-[:SPREADS]->(n:News),
      (u2:User)-[:CREATED|SHARED]->(p2:Post)-[:SPREADS]->(n)
WHERE u1.id < u2.id
  AND EXISTS { MATCH (u1)-[:FOLLOWS|INTERACTS_WITH]-(u2) }
RETURN n.title          AS news,
       n.riskScore      AS riskScore,
       u1.username      AS userA,
       u2.username      AS userB
ORDER BY n.riskScore DESC, news;


// ------------------------------------------------------------
// Q11. Top difusores de desinformación (alto riesgo).
//
// Ranking de usuarios por cantidad de posts vinculados a
// noticias con riskScore alto (>= 70). Útil para mostrar impacto.
// ------------------------------------------------------------
CYPHER 25
MATCH (u:User)-[:CREATED|SHARED]->(p:Post)-[:SPREADS]->(n:News)
WHERE n.riskScore >= 70
RETURN u.username        AS user,
       u.role            AS role,
       u.suspicious      AS flagged,
       count(DISTINCT p) AS riskyPosts,
       count(DISTINCT n) AS uniqueNewsSpread
ORDER BY riskyPosts DESC, uniqueNewsSpread DESC
LIMIT 10;


// ------------------------------------------------------------
// Q12. Temas más afectados por desinformación.
//
// Combina ABOUT con riskScore (escala 0..100, umbral 60) para
// identificar en qué áreas se concentra el contenido dudoso.
// ------------------------------------------------------------
CYPHER 25
MATCH (n:News)-[:ABOUT]->(t:Topic)
WHERE n.riskScore >= 60
RETURN t.name                AS topic,
       count(n)              AS riskyNewsCount,
       round(avg(n.riskScore)) AS avgRiskScore
ORDER BY riskyNewsCount DESC, avgRiskScore DESC;
