// ============================================================
// 05_verification_queries.cypher
// Consultas de verificación — NexoVeraz (App Anti Fake News)
// TP-2 Bases de Datos II
//
// Ejecutar DESPUÉS de 02_seed_data.cypher y 04_additional_news_seed.cypher
// para validar el volumen y la distribución de datos. Son de SOLO LECTURA
// (no modifican la base). Ejecutar cada consulta por separado en Neo4j Query.
// ============================================================


// --- 1) Total de noticias por nivel de riesgo ---
// Esperado (con seed base + adicional): LOW ~12, MEDIUM ~9, HIGH ~9.
MATCH (n:News)
RETURN n.riskLevel AS riskLevel, count(*) AS total
ORDER BY riskLevel;


// --- 2) Total de noticias por tema ---
MATCH (n:News)-[:ABOUT]->(t:Topic)
RETURN t.name AS tema, count(DISTINCT n) AS noticias
ORDER BY noticias DESC, tema;


// --- 3) Total de fuentes por nivel de credibilidad ---
// Alta >= 0.7 · Media 0.4..0.7 · Baja < 0.4
MATCH (s:Source)
RETURN
  CASE
    WHEN s.credibilityScore >= 0.7 THEN 'Alta (>=0.7)'
    WHEN s.credibilityScore >= 0.4 THEN 'Media (0.4-0.7)'
    ELSE 'Baja (<0.4)'
  END AS nivelCredibilidad,
  count(*) AS fuentes
ORDER BY nivelCredibilidad;


// --- 4) Cantidad de noticias asociadas a la cuenta demo ---
// Si devuelve 0, la cuenta demo aún no existe o no se ejecutó el enlace:
// levantar el backend (DemoDataInitializer) o re-ejecutar 04_additional_news_seed.cypher.
MATCH (demo:AppUser {email: 'demo@nexoveraz.local'})-[:OWNS_NEWS]->(n:News)
RETURN count(n) AS noticiasDeLaCuentaDemo;


// --- 5) Noticias MEDIUM existentes (detalle) ---
MATCH (n:News {riskLevel: 'MEDIUM'})
OPTIONAL MATCH (n)-[:PUBLISHED_BY]->(s:Source)
RETURN n.id AS id, n.title AS titulo, n.riskScore AS score, s.name AS fuente
ORDER BY n.riskScore DESC, n.id;


// --- 6) Cantidad de nodos por label ---
MATCH (n)
RETURN labels(n)[0] AS label, count(*) AS total
ORDER BY label;


// --- 7) Cantidad de relaciones por tipo ---
MATCH ()-[r]->()
RETURN type(r) AS relacion, count(*) AS total
ORDER BY relacion;


// ============================================================
// Verificaciones extra (opcionales)
// ============================================================

// --- 8) Distribución conjunta riesgo + cantidad (para dashboard/reportes) ---
MATCH (n:News)
RETURN
  sum(CASE WHEN n.riskLevel = 'LOW'    THEN 1 ELSE 0 END) AS low,
  sum(CASE WHEN n.riskLevel = 'MEDIUM' THEN 1 ELSE 0 END) AS medium,
  sum(CASE WHEN n.riskLevel = 'HIGH'   THEN 1 ELSE 0 END) AS high,
  count(*) AS totalNoticias;

// --- 9) Solo las noticias del seed adicional (origen ADDITIONAL_SEED) ---
MATCH (demo:AppUser {email: 'demo@nexoveraz.local'})-[r:OWNS_NEWS {origin: 'ADDITIONAL_SEED'}]->(n:News)
RETURN n.id AS id, n.riskLevel AS riskLevel, n.title AS titulo
ORDER BY n.id;
