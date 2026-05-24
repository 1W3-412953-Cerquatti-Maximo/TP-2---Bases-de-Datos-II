// ============================================================
// 01_constraints.cypher
// Unique constraints — NexoVeraz (App Anti Fake News)
// TP-2 Bases de Datos II
//
// Ejecutar UNA sola vez sobre la base `antifakenews` antes
// de cargar los datos de prueba. Idempotente: IF NOT EXISTS
// evita errores si los constraints ya existen.
// ============================================================

// --- Nodos principales del modelo de grafo ---

CREATE CONSTRAINT news_id_unique       IF NOT EXISTS FOR (n:News)      REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT source_id_unique     IF NOT EXISTS FOR (s:Source)    REQUIRE s.id IS UNIQUE;
CREATE CONSTRAINT user_id_unique       IF NOT EXISTS FOR (u:User)      REQUIRE u.id IS UNIQUE;
CREATE CONSTRAINT post_id_unique       IF NOT EXISTS FOR (p:Post)      REQUIRE p.id IS UNIQUE;
CREATE CONSTRAINT topic_id_unique      IF NOT EXISTS FOR (t:Topic)     REQUIRE t.id IS UNIQUE;
CREATE CONSTRAINT claim_id_unique      IF NOT EXISTS FOR (c:Claim)     REQUIRE c.id IS UNIQUE;
CREATE CONSTRAINT evidence_id_unique   IF NOT EXISTS FOR (e:Evidence)  REQUIRE e.id IS UNIQUE;
CREATE CONSTRAINT factcheck_id_unique  IF NOT EXISTS FOR (f:FactCheck) REQUIRE f.id IS UNIQUE;

// --- Verificación rápida (opcional) ---
// SHOW CONSTRAINTS;
