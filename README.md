# NexoVeraz — App Anti Fake News

Trabajo Práctico 2 — **Bases de Datos II**.
Aplicación de análisis y trazabilidad de fake news basada en Neo4j (grafo) + backend Java.

> Estado: **Fase 1** completada. Modelo de grafo final consolidado, scripts de carga y consultas de demostración listos. Backend con conexión validada y health checks.

---

## Stack

| Componente        | Versión |
|-------------------|---------|
| Java              | 21      |
| Spring Boot       | 3.3.5   |
| Neo4j Java Driver | 5.18.0  |
| Neo4j             | 5.x     |
| Maven             | 3.9+    |

---

## Modelo de grafo

### Nodos (8)

| Etiqueta    | Propiedades principales                                                           |
|-------------|-----------------------------------------------------------------------------------|
| `News`      | `id`, `title`, `content`, `url`, `publishedAt`, `riskScore` *(escala 0..100)*, `riskLevel` *(`LOW` 0–39 · `MEDIUM` 40–69 · `HIGH` 70–100)* |
| `Source`    | `id`, `name`, `url`, `type`, `credibilityScore` *(0 = no confiable, 1 = altamente confiable)* |
| `User`      | `id`, `username`, `role` *(FACT_CHECKER, JOURNALIST, EDITOR, INFLUENCER, READER, SUSPICIOUS)*, `followerCount`, `suspicious` |
| `Post`      | `id`, `content`, `platform` *(TWITTER, FACEBOOK, INSTAGRAM, TELEGRAM, TIKTOK)*, `createdAt` |
| `Topic`     | `id`, `name`, `slug`                                                              |
| `Claim`     | `id`, `text`, `status` *(VERIFIED, REFUTED, UNDER_REVIEW)*                        |
| `Evidence`  | `id`, `description`, `url`, `type`                                                |
| `FactCheck` | `id`, `verdict` *(TRUE, FALSE, PARTIALLY_TRUE, MISLEADING)*, `explanation`, `confidence`, `publishedAt` |

### Relaciones (con propiedades)

Cada relación lleva metadatos propios para enriquecer las consultas y reflejar el contexto temporal/cuantitativo de cada arista del grafo.

| Relación | Propiedades |
|----------|-------------|
| `News -[:PUBLISHED_BY]-> Source`     | `firstSeenAt`, `sourceUrl` |
| `News -[:CONTAINS]-> Claim`          | `extractedAt`, `extractionMethod` |
| `News -[:ABOUT]-> Topic`             | `relevance` *(0..1)* |
| `User -[:CREATED]-> Post`            | `createdAt`, `deviceType` *(`WEB`, `MOBILE_IOS`, `MOBILE_ANDROID`, `BOT_API`)* |
| `Post -[:SPREADS]-> News`            | `observedAt`, `reach`, `engagementCount` |
| `User -[:SHARED]-> Post`             | `sharedAt`, `shareType` *(`REPOST`, `QUOTE`, `REACTION`)*, `reach` |
| `User -[:FOLLOWS]-> User`            | `since`, `interactionStrength` *(0..1)* |
| `User -[:INTERACTS_WITH]-> User`     | `interactionType` *(`MENTION`, `REPLY`, `QUOTE`)*, `weight` *(0..1)*, `lastInteractionAt` |
| `Claim -[:SUPPORTED_BY]-> Evidence`  | `confidence` *(0..1)*, `note` |
| `Claim -[:REFUTED_BY]-> Evidence`    | `confidence` *(0..1)*, `note` |
| `FactCheck -[:CHECKS]-> Claim`       | `checkedAt`, `method` *(`MANUAL_REVIEW`, `AUTOMATED`)* |
| `FactCheck -[:BASED_ON]-> Evidence`  | `relevance` *(0..1)*, `usedAs` *(`PRIMARY`, `CORROBORATING`)* |

### Caminos centrales del modelo

- **Verificación**: `News -[:CONTAINS]-> Claim <-[:CHECKS]- FactCheck -[:BASED_ON]-> Evidence`
- **Circulación**: `User -[:CREATED|SHARED]-> Post -[:SPREADS]-> News`
- **Difusión coordinada**: dos `User` enlazados por `FOLLOWS`/`INTERACTS_WITH` que terminan apuntando a la misma `News`.

---

## Setup

### 1. Crear la base en Neo4j Desktop

1. Abrir Neo4j Desktop y crear un proyecto.
2. Agregar una **Local DBMS** (Neo4j 5.x) y arrancarla.
3. Abrir **Neo4j Query** (o Neo4j Browser).
4. Crear la base:

```cypher
CREATE DATABASE antifakenews IF NOT EXISTS;
:use antifakenews
```

### 2. Configurar variables de entorno

Copiar `.env.example` a `.env` y completar tu contraseña:

```
NEO4J_URI=neo4j://127.0.0.1:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=tu_contraseña_aqui
NEO4J_DATABASE=antifakenews
```

> **IntelliJ IDEA**: *Run > Edit Configurations > Environment variables* → cargar las 4 variables. Alternativamente, instalar el plugin **EnvFile** para leer `.env` automáticamente.
>
> **PowerShell** (sesión actual):
> ```powershell
> $env:NEO4J_PASSWORD = "tu_contraseña_aqui"
> ```

### 3. Ejecutar el backend

```bash
cd backend-java
mvn spring-boot:run
```

Servidor en `http://localhost:8080`.

### 4. Probar los endpoints

**Health checks:**

```bash
curl http://localhost:8080/api/health
# {"status":"OK"}

curl http://localhost:8080/api/health/neo4j
# {"status":"OK","result":1}
```

Lista completa de endpoints REST en la siguiente sección.

---

## Cargar los scripts Cypher

Abrir **Neo4j Query**, seleccionar la base `antifakenews` y ejecutar **en orden**:

| Paso | Script                          | Qué hace                                            |
|------|---------------------------------|-----------------------------------------------------|
| 0 *(opcional, solo dev)* | `cypher/00_reset_database.cypher` | ⚠️ Borra TODOS los nodos y relaciones. Útil para re-cargar el seed limpio en local. **Nunca en producción.** |
| 1    | `cypher/01_constraints.cypher`  | Crea constraints únicos para los 8 tipos de nodo    |
| 2    | `cypher/02_seed_data.cypher`    | Carga el dataset ficticio (8 noticias, 6 fuentes, 10 usuarios, 13 posts, 6 temas, 8 claims, 6 evidencias, 5 fact checks) **con propiedades en nodos y en relaciones** |
| 3    | `cypher/03_queries.cypher`      | Consultas de demostración para la defensa del TP   |

> Los scripts 01, 02 y 03 son **idempotentes** (`IF NOT EXISTS` + `MERGE` + `SET`): se pueden re-ejecutar sin duplicar nodos. El script 00 es **destructivo** — usar solo en desarrollo local antes de re-cargar el seed.
>
> **Sobre `riskScore`**: en `News` la propiedad va de **0 a 100** (no 0 a 1). El nivel categórico `riskLevel` queda derivado: `LOW` 0–39, `MEDIUM` 40–69, `HIGH` 70–100.

### Verificación rápida después del paso 2

**Nodos por etiqueta:**

```cypher
MATCH (n) RETURN labels(n)[0] AS label, count(*) AS total ORDER BY label;
```

Esperado:

| label       | total |
|-------------|-------|
| Claim       | 8     |
| Evidence    | 6     |
| FactCheck   | 5     |
| News        | 8     |
| Post        | 13    |
| Source      | 6     |
| Topic       | 6     |
| User        | 10    |

**Relaciones por tipo:**

```cypher
MATCH ()-[r]->() RETURN type(r) AS rel, count(*) AS total ORDER BY rel;
```

**Confirmar que las relaciones llevan propiedades** (no solo los nodos):

```cypher
MATCH (n:News)-[r:PUBLISHED_BY]->(s:Source)
RETURN n.title, properties(r) AS relProps LIMIT 3;

MATCH (p:Post)-[r:SPREADS]->(n:News)
RETURN p.id, n.title, r.reach, r.engagementCount LIMIT 5;

MATCH (a:User)-[r:FOLLOWS]->(b:User)
RETURN a.username, b.username, r.since, r.interactionStrength LIMIT 5;
```

### Consultas incluidas en `03_queries.cypher`

1. Noticias con su fuente y confiabilidad
2. Noticias de alto riesgo (`riskScore >= 0.7`)
3. Noticias agrupadas por tema
4. Fuentes con baja confiabilidad
5. Usuarios que difundieron noticias riesgosas
6. Claims refutados con su veredicto y evidencias
7. Evidencias asociadas a una noticia puntual
8. Recorrido completo `News → Claim → FactCheck → Evidence`
9. Propagación `User → Post → News`
10. Usuarios conectados que difundieron la misma noticia
11. Top difusores de desinformación
12. Temas más afectados por desinformación

---

## Estructura del proyecto

```
.
├── backend-java/
│   ├── pom.xml
│   └── src/main/java/com/antifakenews/
│       ├── AntiFakeNewsApplication.java
│       ├── config/
│       │   ├── Neo4jConfig.java             # Driver + SessionConfig beans
│       │   └── WebConfig.java               # CORS para http://localhost:4200
│       ├── controller/                       # NewsController, SourceController,
│       │                                     # TopicController, GraphController,
│       │                                     # DashboardController, HealthController
│       ├── service/                          # capa de servicios (delegan a repositories)
│       ├── repository/                       # Cypher ejecutado vía Neo4j Driver
│       ├── dto/                              # records (NewsSummaryDto, NewsDetailDto, etc.)
│       └── exception/                        # NotFoundException + GlobalExceptionHandler
├── cypher/
│   ├── 00_reset_database.cypher             # ⚠️ Solo desarrollo: vacía la base
│   ├── 01_constraints.cypher                # Constraints únicos (8 nodos)
│   ├── 02_seed_data.cypher                  # Dataset ficticio (props en nodos y relaciones)
│   └── 03_queries.cypher                    # Consultas de demostración
├── .env.example
├── .gitignore
└── README.md
```

---

## API REST

CORS habilitado para `http://localhost:4200` (Angular en próxima fase).

| Método | Endpoint                      | Descripción                                              |
|--------|-------------------------------|----------------------------------------------------------|
| GET    | `/api/health`                 | Health check básico                                       |
| GET    | `/api/health/neo4j`           | Conexión Neo4j (ejecuta `RETURN 1`)                       |
| GET    | `/api/news`                   | Listado de noticias (resumen + fuente + temas)            |
| GET    | `/api/news/{id}`              | Detalle: noticia + fuente + temas + claims + evidencias + fact checks + posts + usuarios |
| GET    | `/api/news/{id}/analysis`     | Análisis de riesgo explicable (señales + score + nivel)   |
| GET    | `/api/sources`                | Lista de fuentes con `credibilityScore`                   |
| GET    | `/api/topics`                 | Lista de temas                                            |
| GET    | `/api/graph/news/{id}`        | Grafo `{nodes, edges}` con propiedades de relaciones      |
| GET    | `/api/dashboard/summary`      | Conteos agregados (totales + riskLevel)                   |

Errores: `404` cuando la noticia no existe, con cuerpo JSON `{status, error, message, timestamp}`.

### Ejemplos curl

```bash
# Listado de noticias
curl http://localhost:8080/api/news

# Detalle de una noticia
curl http://localhost:8080/api/news/news-003

# Grafo de una noticia (para visualización)
curl http://localhost:8080/api/graph/news/news-003

# Fuentes
curl http://localhost:8080/api/sources

# Temas
curl http://localhost:8080/api/topics

# Dashboard
curl http://localhost:8080/api/dashboard/summary

# Caso 404
curl -i http://localhost:8080/api/news/news-does-not-exist
```

**Respuesta esperada `/api/dashboard/summary`** (con el seed actual):

```json
{
  "totalNews": 8,
  "highRiskNews": 3,
  "mediumRiskNews": 0,
  "lowRiskNews": 5,
  "totalSources": 6,
  "totalClaims": 8,
  "totalFactChecks": 5,
  "totalPosts": 13,
  "totalUsers": 10
}
```

**Estructura `/api/graph/news/{id}`:**

```json
{
  "nodes": [
    { "id": "news-003", "label": "News",   "title": "El 5G estaría provocando..." },
    { "id": "src-005",  "label": "Source", "title": "VerdadOculta" },
    { "id": "topic-002","label": "Topic",  "title": "Salud" }
  ],
  "edges": [
    { "from": "news-003", "to": "src-005",  "type": "PUBLISHED_BY", "properties": { "firstSeenAt": "2026-03-14T18:45Z", "sourceUrl": "..." } },
    { "from": "news-003", "to": "topic-002","type": "ABOUT",        "properties": { "relevance": 0.6 } }
  ]
}
```

---

## Análisis de riesgo (Fase 3)

**Endpoint:** `GET /api/news/{id}/analysis`

El análisis es **determinístico y explicable** — no usa IA, no usa probabilidades opacas. Cada punto sumado corresponde a una señal detectada en el grafo, identificada por un `code`, una descripción en español y los puntos que aporta. El resultado nunca afirma "esta noticia es falsa"; informa que **presenta un nivel de riesgo `LOW`, `MEDIUM` o `HIGH` según las señales detectadas**.

> Como efecto colateral, el endpoint persiste `riskScore` y `riskLevel` calculados sobre el nodo `:News`. Como las reglas son determinísticas, re-ejecutar el análisis produce el mismo valor.

### Reglas de scoring

| # | Señal (`code`)                  | Disparador                                                                                          | Puntos |
|---|---------------------------------|-----------------------------------------------------------------------------------------------------|-------:|
| 1 | `LOW_CREDIBILITY_SOURCE`        | La `Source` publicadora tiene `credibilityScore < 0.4`                                              | **25** |
| 2 | `FALSE_FACT_CHECK`              | Algún `Claim` de la noticia tiene `FactCheck` con `verdict = 'FALSE'`                               | **40** |
| 3 | `MISLEADING_FACT_CHECK`         | Algún `Claim` tiene `FactCheck` con `verdict = 'MISLEADING'`                                        | **25** |
| 4 | `CLAIM_REFUTED_BY_EVIDENCE`     | Algún `Claim` tiene relación `REFUTED_BY` con una `Evidence`                                        | **30** |
| 5 | `CLAIM_WITHOUT_EVIDENCE`        | Algún `Claim` no tiene `SUPPORTED_BY` ni `REFUTED_BY`                                               | **20** |
| 6 | `HIGH_PROPAGATION_VOLUME`       | Más de 3 `Post` con `[:SPREADS]→` esta noticia                                                       | **10** |
| 7 | `CONNECTED_USERS_PROPAGATION`   | Dos `User` enlazados por `FOLLOWS`/`INTERACTS_WITH` difunden la misma noticia vía sus posts          | **15** |
| 8 | `HIGH_REACH_POST`               | Algún post con `[:SPREADS {reach > 20000}]`                                                          | **10** |

### Cálculo del nivel

```
finalScore = min(suma_de_puntos, 100)

riskLevel = LOW     si finalScore ∈ [0,  39]
            MEDIUM  si finalScore ∈ [40, 69]
            HIGH    si finalScore ∈ [70, 100]
```

### Ejemplos curl

```bash
curl http://localhost:8080/api/news/news-001/analysis
curl http://localhost:8080/api/news/news-003/analysis
curl http://localhost:8080/api/news/news-007/analysis

# 404 esperado
curl -i http://localhost:8080/api/news/no-existe/analysis
```

**Respuesta esperada `news-003`** (alto riesgo, capeado a 100):

```json
{
  "newsId": "news-003",
  "title": "El 5G estaría provocando una epidemia de dolores de cabeza",
  "riskScore": 100,
  "riskLevel": "HIGH",
  "summary": "Esta noticia presenta alto riesgo porque proviene de una fuente de baja confiabilidad, contiene afirmaciones marcadas como falsas por fact-check, contiene afirmaciones refutadas por evidencia, tiene un volumen alto de publicaciones que la difunden, fue difundida por usuarios conectados entre sí y fue amplificada por publicaciones de alto alcance.",
  "signals": [
    { "code": "LOW_CREDIBILITY_SOURCE",      "description": "La fuente tiene baja confiabilidad histórica (credibilityScore < 0.4).", "points": 25 },
    { "code": "FALSE_FACT_CHECK",            "description": "Al menos un fact-check asociado emitió veredicto FALSE.",                  "points": 40 },
    { "code": "CLAIM_REFUTED_BY_EVIDENCE",   "description": "Al menos un claim de la noticia está refutado por evidencia (REFUTED_BY).","points": 30 },
    { "code": "HIGH_PROPAGATION_VOLUME",     "description": "La noticia tiene más de 3 publicaciones que la difunden.",                 "points": 10 },
    { "code": "CONNECTED_USERS_PROPAGATION", "description": "Existen usuarios conectados (FOLLOWS / INTERACTS_WITH) que difundieron la misma noticia.", "points": 15 },
    { "code": "HIGH_REACH_POST",             "description": "Al menos una publicación que la difunde alcanzó más de 20.000 personas.", "points": 10 }
  ]
}
```

**Respuesta esperada `news-001`** (bajo riesgo, una sola señal):

```json
{
  "newsId": "news-001",
  "title": "Gobierno presenta plan económico de tres etapas",
  "riskScore": 15,
  "riskLevel": "LOW",
  "summary": "Esta noticia presenta bajo riesgo porque fue difundida por usuarios conectados entre sí.",
  "signals": [
    { "code": "CONNECTED_USERS_PROPAGATION", "description": "Existen usuarios conectados (FOLLOWS / INTERACTS_WITH) que difundieron la misma noticia.", "points": 15 }
  ]
}
```

**Importante para la defensa del TP:**

- El análisis **no determina verdad absoluta**: clasifica riesgo según señales observables en el grafo.
- Es **trazable**: cada punto del `riskScore` está asociado a una señal con su código y descripción.
- Es **reproducible**: las mismas reglas sobre el mismo grafo siempre dan el mismo resultado.
- No usa IA, ni embeddings, ni modelos de ML. Es lógica de negocio explícita.

---

## Frontend Angular (Fase 4)

SPA en `frontend-angular/` con identidad visual NexoVeraz.

### Levantarlo

```bash
cd frontend-angular
npm install        # primera vez, ~2-3 min
npm start          # ng serve en http://localhost:4200
```

Requiere que el backend esté corriendo en `http://localhost:8080` (CORS ya habilitado solo para `localhost:4200`).

### Pantallas

| Ruta            | Contenido                                                                  |
|-----------------|----------------------------------------------------------------------------|
| `/dashboard`    | Tarjetas LOW/MEDIUM/HIGH + conteos + intro del proyecto                    |
| `/news`         | Tabla de noticias con score, nivel y temas                                 |
| `/news/:id`     | Detalle + botón **Calcular análisis de riesgo** + grafo de relaciones      |
| `/sources`      | Tabla de fuentes con barra de `credibilityScore`                           |
| `/reports`      | Distribución por riesgo, fuentes ordenadas por credibilidad, top risk     |

Detalles técnicos completos en [`frontend-angular/README.md`](frontend-angular/README.md).

---

## Roadmap

- [x] **Fase 1** — Modelo de grafo, scripts Cypher, conexión Java↔Neo4j validada.
- [x] **Fase 2** — Endpoints REST sobre el modelo (lectura por nodo + recorridos + grafo + dashboard) + CORS.
- [x] **Fase 3** — Análisis de riesgo determinístico y explicable (`/api/news/{id}/analysis`).
- [x] **Fase 4** — Frontend Angular SPA en `http://localhost:4200`, identidad NexoVeraz.
