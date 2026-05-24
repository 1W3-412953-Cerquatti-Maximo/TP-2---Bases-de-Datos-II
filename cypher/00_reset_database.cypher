// ============================================================
// 00_reset_database.cypher
// ⚠️  ATENCIÓN — script DESTRUCTIVO
//
// Elimina TODOS los nodos y relaciones de la base `antifakenews`.
// Los constraints definidos en 01_constraints.cypher NO se eliminan
// (solo se pierden los datos, no el esquema).
//
// USAR EXCLUSIVAMENTE EN DESARROLLO/LOCAL antes de volver a
// cargar el seed limpio. Flujo típico:
//
//     1. Ejecutar este script (vaciar la base)
//     2. Ejecutar 02_seed_data.cypher (re-cargar dataset)
//
// NO ejecutar en una base con datos que no se puedan perder.
// ============================================================

MATCH (n) DETACH DELETE n;

// Verificación: debería devolver 0
// MATCH (n) RETURN count(n) AS remainingNodes;
