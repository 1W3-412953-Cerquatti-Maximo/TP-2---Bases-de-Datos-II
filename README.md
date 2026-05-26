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
| 2    | `cypher/02_seed_data.cypher`    | Carga el dataset ficticio base (8 noticias, 6 fuentes, 10 usuarios, 13 posts, 6 temas, 8 claims, 6 evidencias, 5 fact checks) **con propiedades en nodos y en relaciones** |
| 3    | `cypher/03_queries.cypher`      | Consultas de demostración para la defensa del TP   |
| 4 *(opcional, recomendado para demo)* | `cypher/04_additional_news_seed.cypher` | **Aditivo**: agrega 22 noticias más (`news-009`..`news-030`) con variedad de riesgo (**LOW, MEDIUM y HIGH**), 8 fuentes, 6 temas, 22 claims, 14 evidencias, 12 fact checks, 25 posts y 14 usuarios. Asocia todas las noticias nuevas a la cuenta demo. |
| 5 *(verificación)* | `cypher/05_verification_queries.cypher` | Consultas de solo lectura para validar volumen y distribución (riesgo, temas, credibilidad, conteos). |

> Los scripts 01, 02, 03 y 04 son **idempotentes** (`IF NOT EXISTS` + `MERGE` + `SET`): se pueden re-ejecutar sin duplicar nodos. El script 00 es **destructivo** — usar solo en desarrollo local antes de re-cargar el seed.
>
> **Sobre `riskScore`**: en `News` la propiedad va de **0 a 100** (no 0 a 1). El nivel categórico `riskLevel` queda derivado: `LOW` 0–39, `MEDIUM` 40–69, `HIGH` 70–100.
>
> **Sobre el seed adicional (`04`)**: es **opcional pero recomendado para la demo** porque suma volumen y, sobre todo, noticias **MEDIUM** (el seed base no tiene ninguna), que ahora se ven en dashboard y reportes. Tras cargarlo la base queda con ~30 noticias (≈12 LOW, 9 MEDIUM, 9 HIGH) y mayor variedad de fuentes y temas. Las 22 noticias nuevas quedan asociadas a la **cuenta demo** (`demo@nexoveraz.local`) vía `OWNS_NEWS`. Si la cuenta demo todavía no existe, levantá el backend una vez (lo crea `DemoDataInitializer`) o re-ejecutá el script `04` después; es idempotente.

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
│   ├── 02_seed_data.cypher                  # Dataset ficticio base (props en nodos y relaciones)
│   ├── 03_queries.cypher                    # Consultas de demostración
│   ├── 04_additional_news_seed.cypher       # Seed aditivo: +22 noticias (LOW/MEDIUM/HIGH) → cuenta demo
│   └── 05_verification_queries.cypher       # Consultas de verificación (solo lectura)
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
| GET    | `/api/dashboard/summary`      | Conteos agregados (totales + riskLevel + credibilidad)    |
| GET    | `/api/dashboard/topic-risk-ranking` | Top 5 temas por riesgo promedio                     |
| GET    | `/api/dashboard/risk-signals` | Top señales de riesgo más frecuentes (count por código)   |
| GET    | `/api/dashboard/news-timeline`| Noticias por día y nivel de riesgo (últimos 14 días)      |
| GET    | `/api/dashboard/graph-summary`| Resumen del subgrafo: conteo por tipo de nodo + relaciones|

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

## Asistente IA (Fase 6)

Integración de IA **modular, opcional y fácil de desactivar**. Por defecto está **apagada**.

> **Qué NO hace la IA:** no decide si una noticia es falsa, no calcula el `riskScore` final y no reemplaza el análisis de Neo4j. El análisis de riesgo sigue siendo 100% determinístico y basado en señales del grafo (Fase 3).
>
> **Qué hace:** resume el texto, sugiere claims, sugiere temas y genera advertencias preliminares — todo como ayuda, sin persistir nada automáticamente.

### Arquitectura

- `ai/AiAnalysisPort` — interfaz (puerto).
- `ai/DisabledAiAnalysisService` — default, responde `enabled:false`.
- `ai/MockAiAnalysisService` — IA simulada con reglas locales (sin API externa).
- `ai/ExternalAiAnalysisService` — estructura preparada para un proveedor externo futuro; si faltan credenciales, falla de forma controlada (no rompe).
- `config/AiConfig` — selecciona la implementación según configuración.

### Configuración

| Variable de entorno    | Default     | Valores                          |
|------------------------|-------------|----------------------------------|
| `AI_ENABLED`           | `false`     | `true` / `false`                 |
| `AI_PROVIDER`          | `disabled`  | `disabled` / `mock` / `external` |
| `AI_EXTERNAL_API_KEY`  | *(vacío)*   | clave del proveedor (futuro)     |
| `AI_EXTERNAL_ENDPOINT` | *(vacío)*   | URL del proveedor (futuro)       |

**Dejar IA apagada (default):** no configurar nada, o `AI_ENABLED=false`.

**Usar modo mock:**
```powershell
$env:AI_ENABLED = "true"
$env:AI_PROVIDER = "mock"
# luego: mvn spring-boot:run
```

**Conectar un proveedor externo (futuro):** poner `AI_PROVIDER=external` y definir `AI_EXTERNAL_API_KEY` + `AI_EXTERNAL_ENDPOINT`. La clase `ExternalAiAnalysisService` ya tiene el punto de extensión para la llamada HTTP real; mientras no esté implementada o falten credenciales, responde de forma controlada sin afectar el resto del sistema. **Las API keys nunca se hardcodean** — solo se leen de variables de entorno.

### Endpoint

`POST /api/ai/analyze-news-text`

```bash
# Request
curl -X POST http://localhost:8080/api/ai/analyze-news-text \
  -H "Content-Type: application/json" \
  -d '{"title":"El 5G provoca una cura milagrosa","content":"Fuentes anónimas aseguran un colapso inminente."}'
```

**Respuesta con IA apagada (default):**
```json
{
  "enabled": false,
  "provider": "disabled",
  "summary": "El asistente de IA está desactivado. El análisis de riesgo se realiza de forma determinística sobre el grafo Neo4j.",
  "suggestedClaims": [],
  "suggestedTopics": [],
  "warnings": []
}
```

**Respuesta en modo mock** (`AI_ENABLED=true`, `AI_PROVIDER=mock`):
```json
{
  "enabled": true,
  "provider": "mock",
  "summary": "Resumen automático (mock): Fuentes anónimas aseguran un colapso inminente.",
  "suggestedClaims": ["El 5G provoca una cura milagrosa", "Fuentes anónimas aseguran un colapso inminente."],
  "suggestedTopics": ["Ciencia y Tecnología", "Salud"],
  "warnings": [
    "Lenguaje alarmista detectado ('colapso'): verificar si la afirmación está respaldada o es sensacionalista.",
    "Afirmación extraordinaria ('cura milagrosa') sin evidencia: requiere fuentes científicas verificables."
  ]
}
```

En el frontend, la sección **"Asistente IA"** del detalle de noticia consume este endpoint: muestra claramente si la IA está apagada, y si está activa lista summary / temas / claims / warnings — sin mezclarse con el `riskScore` determinístico.

---

## Autenticación demo (Fase 7.1)

Auth **para demo académica**, no producción. Login/registro con token simple.

> - **No** usa la cadena de filtros de Spring Security (solo `spring-security-crypto` para BCrypt).
> - Las contraseñas se guardan **hasheadas con BCrypt** — nunca en texto plano.
> - El token es un **UUID** guardado como `:AuthSession` en Neo4j (no es JWT). El frontend lo envía como `Authorization: Bearer <token>`.
> - Pensado para el TP: simple y trazable, sin seguridad de grado productivo.

### Modelo Neo4j

```
(:AppUser { id, username, email, displayName, passwordHash, role, themePreference, createdAt })
(:AuthSession { token, createdAt, expiresAt })
(:AppUser)-[:HAS_SESSION]->(:AuthSession)
```

`AppUser` es el usuario de la app (login), distinto del nodo `:User` del grafo de circulación de noticias. Los constraints están en `cypher/01_constraints.cypher` (re-ejecutarlo agrega los nuevos sin duplicar).

### Endpoints

| Método | Endpoint                     | Auth | Descripción                                  |
|--------|------------------------------|------|----------------------------------------------|
| POST   | `/api/auth/register`         | —    | Crea usuario + sesión, devuelve token        |
| POST   | `/api/auth/login`            | —    | Valida credenciales, devuelve token          |
| GET    | `/api/auth/me`               | Bearer | Usuario actual a partir del token          |
| PUT    | `/api/auth/me/preferences`   | Bearer | Cambia `themePreference` (`dark`/`light`)  |
| POST   | `/api/auth/logout`           | Bearer | Borra la sesión (opcional)                 |

Errores: `409` email/username ya registrado · `401` credenciales o token inválidos · `400` campos faltantes.

### Probar con curl

```bash
# 1) Registro (devuelve token + user)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"maximo","email":"maximo@nexoveraz.local","displayName":"Máximo","password":"123456"}'

# 2) Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maximo@nexoveraz.local","password":"123456"}'

# 3) Usuario actual (reemplazar <TOKEN> por el token recibido)
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"

# 4) Cambiar tema
curl -X PUT http://localhost:8080/api/auth/me/preferences \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"themePreference":"light"}'

# 5) Logout
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <TOKEN>"
```

**Respuesta de register/login (`AuthResponse`):**
```json
{
  "token": "f1e2d3c4-...",
  "user": {
    "id": "...", "username": "maximo", "email": "maximo@nexoveraz.local",
    "displayName": "Máximo", "role": "USER", "themePreference": "dark"
  }
}
```

---

## Flujo protegido y pertenencia de noticias (Fase 7.3)

A partir de esta fase la app **requiere sesión** y cada usuario ve **solo sus propias noticias**.

### Relación de pertenencia

```
(:AppUser)-[:OWNS_NEWS { createdAt, origin }]->(:News)
```

Define qué noticias puede ver cada usuario. Todas las consultas de datos parten del usuario autenticado:

```cypher
MATCH (owner:AppUser {id: $userId})-[:OWNS_NEWS]->(n:News)
// ... y desde ahí se recorre el resto del grafo
```

### Cuenta demo

Un inicializador (`DemoDataInitializer`, activable con `demo.seed.enabled`, default `true`) crea/actualiza al arrancar la cuenta demo y le **vincula todas las noticias seed** sin dueño:

| Campo    | Valor                   |
|----------|-------------------------|
| email    | `demo@nexoveraz.local`  |
| password | `123456`                |
| username | `demo`                  |
| rol      | `USER`                  |

> La contraseña se guarda **hasheada con BCrypt** — nunca en texto plano. El proceso es idempotente (no duplica usuario ni relaciones).

**Una cuenta nueva arranca sin noticias**: dashboard en cero, listado/reportes/fuentes vacíos hasta que cargue las suyas (Fase 8).

### Endpoints protegidos vs públicos

| Público                        | Protegido (requiere `Authorization: Bearer <token>`) |
|--------------------------------|-------------------------------------------------------|
| `GET /api/health`              | `GET /api/dashboard/summary`                          |
| `GET /api/health/neo4j`        | `GET /api/news`, `GET /api/news/{id}`                 |
| `POST /api/auth/register`      | `GET /api/news/{id}/analysis`                         |
| `POST /api/auth/login`         | `GET /api/graph/news/{id}`                            |
|                                | `GET /api/sources`, `GET /api/topics`                 |
|                                | `POST /api/ai/analyze-news-text`                      |

Sin token (o token inválido) → **401**. Noticia que no pertenece al usuario → **404** (no se filtra su existencia).

La validación de token la centraliza `AuthenticatedUserResolver` (lee el header Bearer, resuelve el usuario; no usa la cadena de filtros de Spring Security).

### Probar con curl

```bash
# 1) Login demo → copiar el token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@nexoveraz.local","password":"123456"}'

# 2) Listar noticias del usuario (con token)
curl http://localhost:8080/api/news -H "Authorization: Bearer <TOKEN>"

# 3) Sin token → 401
curl -i http://localhost:8080/api/news

# 4) Dashboard del usuario
curl http://localhost:8080/api/dashboard/summary -H "Authorization: Bearer <TOKEN>"
```

---

## Noticias por URL: alta, borrado y temas (Fase 8.2)

Cada usuario puede **cargar noticias desde una URL** y **eliminarlas**. Todo opera sobre su subgrafo (`OWNS_NEWS`) y requiere `Authorization: Bearer <token>`.

### Endpoints

| Método | Endpoint                 | Descripción                                                                 |
|--------|--------------------------|-----------------------------------------------------------------------------|
| POST   | `/api/news/submit-url`   | Crea una noticia desde una URL (extracción best-effort + temas + riesgo)    |
| DELETE | `/api/news/{id}`         | Elimina una noticia propia (404 si no existe o no le pertenece)             |

### Persistencia del `riskScore` evaluado

`POST /api/news/submit-url` acepta opcionalmente `riskScore`, `riskLevel` y `evaluationSummary`. Esto resuelve la incongruencia de que una noticia evaluada con score 20 se guardara con score 0:

- **Con `riskScore`**: se clampea a `[0, 100]`; el `riskLevel` se valida (`LOW`/`MEDIUM`/`HIGH`) o se deriva del score (`0–39 LOW`, `40–69 MEDIUM`, `70–100 HIGH`); el `status` queda en **`EVALUATED`**.
- **Sin `riskScore`**: `riskScore = 0`, `riskLevel = LOW`, `status = PENDING_ANALYSIS`.

El frontend ("Evaluar link") reenvía el `riskScore`/`riskLevel` que devolvió la evaluación, de modo que `/news` muestra el mismo score. El análisis determinístico del grafo sigue disponible aparte vía `GET /api/news/{id}/analysis`.

### Asociación automática de temas

`TopicSuggestionService` asegura que toda noticia tenga al menos un tema:

1. Primero respeta los `topicNames` enviados por el frontend/IA (marcados `source: USER_OR_AI`).
2. Luego agrega temas detectados por palabras clave sobre título + contenido + URL + fuente (marcados `source: AUTO`), normalizando acentos: **Salud, Tecnología, Economía, Política, Clima, Seguridad**.
3. Si no se detecta ninguno, asocia **`General`**.

Cada tema se vincula con `MERGE (t:Topic {name})` + `(:News)-[:ABOUT {relevance: 0.5, source}]->(t)`. La respuesta incluye `topicNames`, y los temas se ven en `/news` y `/news/{id}`.

### Borrado y fuente huérfana

`DELETE /api/news/{id}` valida pertenencia (`(:AppUser {id})-[:OWNS_NEWS]->(:News {id})`) y luego:

- `DETACH DELETE` de la `News` (quita `OWNS_NEWS`, `PUBLISHED_BY`, `ABOUT`, `CONTAINS`…).
- Borra los `Post` que difundían **exclusivamente** esa noticia (no se tocan los que difunden otras).
- Borra la `Source` **solo si** no le queda ninguna otra `News` asociada globalmente (fuente huérfana). Si la comparten otras noticias, se conserva.
- **No** elimina `AppUser`, `Topic`, `Claim`, `Evidence` ni `FactCheck` (pueden ser compartidos).

Respuesta `DeleteNewsResponse`:

```json
{ "newsId": "...", "deleted": true, "sourceDeleted": true, "sourceName": "ejemplo.com" }
```

En el frontend, `/news` y `/news/{id}` ofrecen un botón **Eliminar** rojo con modal de confirmación; al confirmar, el listado se actualiza en memoria (sin volver a pedir `/api/news`) y el detalle redirige a `/news`. Si se eliminó la fuente, se avisa.

### IDs técnicos ocultos en la UI

Los identificadores internos (`news-003`, `src-002`, UUIDs) ya **no se muestran** en listado, detalle, fuentes ni en la tarjeta de éxito de "Evaluar link". Se siguen usando internamente para el ruteo; el cambio es solo visual.

---

## Dashboard analítico (Fase 8.3)

El Dashboard pasó de tarjetas de números a un panel analítico. Se quitó el bloque "Acceso rápido — noticias de alto riesgo". Todos los datos se filtran por usuario (`OWNS_NEWS`) y los endpoints nuevos requieren `Authorization: Bearer <token>`.

| Visualización | Endpoint | Cómo se calcula |
|---------------|----------|-----------------|
| **Distribución de riesgo** (dona) | `/api/dashboard/summary` | Conteo de noticias del usuario por `riskLevel`. |
| **Confiabilidad de fuentes** (dona) | `/api/dashboard/summary` | Fuentes del usuario por tramo de `credibilityScore` (alta ≥0.7 · media 0.4–0.7 · baja <0.4). |
| **Top temas con mayor riesgo promedio** (barras) | `/api/dashboard/topic-risk-ranking` | `avg(News.riskScore)` por `Topic` sobre `(:AppUser)-[:OWNS_NEWS]->(:News)-[:ABOUT]->(:Topic)`, orden desc, top 5. Color por promedio (verde/amarillo/rojo). |
| **Señales de riesgo más frecuentes** (barras) | `/api/dashboard/risk-signals` | Por cada noticia del usuario evalúa las mismas reglas que `NewsAnalysisService` (fuente baja, fact-check FALSE/MISLEADING, claim refutado, claim sin evidencia, alta propagación, usuarios conectados, post de alto alcance) y cuenta en cuántas aparece cada una. Top 5 con count>0. |
| **Evolución temporal por riesgo** (barras apiladas) | `/api/dashboard/news-timeline` | Agrupa por día (`coalesce(createdAt, publishedAt)`) separando LOW/MEDIUM/HIGH; últimos 14 días con datos, orden ascendente. |
| **Resumen del grafo** (mini grafo SVG) | `/api/dashboard/graph-summary` | Conteo por tipo de nodo del subgrafo del usuario (News, Source, Topic, Claim, Evidence, FactCheck, Post, User) + total de relaciones internas entre esos nodos. |

Todas tienen estado vacío propio (p. ej. "Todavía no hay temas suficientes para analizar."). Las visualizaciones extra cargan de forma independiente: si un endpoint falla, el resto del Dashboard sigue funcionando. Implementadas con **CSS/SVG simple**, sin librerías de charts.

---

## Guion de demo

Flujo recomendado para presentar el TP (~5 minutos).

### 0. Setup previo

```bash
# Neo4j Desktop levantado, base `antifakenews` con los 3 scripts Cypher cargados.

# Terminal 1 — backend
cd backend-java
mvn spring-boot:run            # http://localhost:8080

# Terminal 2 — frontend
cd frontend-angular
npm install                    # solo la primera vez
npm start                      # http://localhost:4200
```

Abrir **http://localhost:4200**.

### 1. Dashboard (30 s)
- Mostrar el bloque intro de NexoVeraz y el slogan.
- Tarjetas LOW / MEDIUM / HIGH con la distribución de riesgo.
- Conteos de los 8 tipos de nodo del grafo.
- Señalar el bloque **Acceso rápido — noticias de alto riesgo**.

### 2. Lista de noticias (45 s)
- Sidebar → **Noticias**.
- Recorrer la tabla: cada fila muestra fuente, temas (chips), barra de `riskScore` y badge de nivel.

### 3. Detalle de `news-003` (1 min)
- Abrir *"El 5G estaría provocando una epidemia de dolores de cabeza"*.
- Hero: título, badge de riesgo, fuente **VerdadOculta** (baja credibilidad), barra de score.
- Recorrer las secciones: fuente, temas, claims, evidencias, fact checks, posts, usuarios.

### 4. Ejecutar análisis de riesgo (1 min)
- Botón **Calcular análisis de riesgo**.
- Leer el disclaimer: *el sistema no afirma que sea falsa, clasifica riesgo*.
- Leer el `summary` y recorrer las **señales detectadas** (cada una con su código y puntos).
- Mensaje clave: cada punto del score es trazable a una señal. No es IA, es lógica explícita.

### 5. Grafo de relaciones (1 min)
- Scroll a **Grafo de relaciones**.
- Mostrar la **leyenda de colores** y los contadores de nodos / relaciones.
- Recorrer los grupos por tipo de nodo (News, Source, Topic, Claim, Evidence, FactCheck, Post, User).
- Mostrar la **tabla de aristas** con sus propiedades (`reach`, `engagementCount`, `sharedAt`, etc.).

### 6. Reportes (45 s)
- Sidebar → **Reportes**.
- Distribución por riesgo (barras).
- Top 5 noticias por `riskScore`.
- **Fuentes menos confiables** — el origen del +25 al riesgo.

### Cierre
- NexoVeraz **no afirma** que una noticia sea falsa.
- Clasifica **riesgo** según señales objetivas del grafo.
- Es **trazable**, **determinístico** y **explicable**.

---

## Roadmap

- [x] **Fase 1** — Modelo de grafo, scripts Cypher, conexión Java↔Neo4j validada.
- [x] **Fase 2** — Endpoints REST sobre el modelo (lectura por nodo + recorridos + grafo + dashboard) + CORS.
- [x] **Fase 3** — Análisis de riesgo determinístico y explicable (`/api/news/{id}/analysis`).
- [x] **Fase 4** — Frontend Angular SPA en `http://localhost:4200`, identidad NexoVeraz.
- [x] **Fase 5** — Pulido visual: leyenda del grafo, disclaimer de análisis, reportes con explicaciones, acceso rápido a HIGH risk, guion de demo.
- [x] **Fase 6** — Asistente IA modular y opcional (`disabled`/`mock`/`external`), apagado por defecto. No decide falsedad ni calcula riskScore.
- [x] **Fase 7.1** — Backend de autenticación demo: register/login/me/preferences con BCrypt + token UUID en Neo4j (`:AppUser`, `:AuthSession`).
- [x] **Fase 7.2** — Frontend de auth: pantallas Login/Register/Profile, interceptor Bearer, guard en `/profile`, y toggle de tema claro/oscuro sincronizado con la cuenta.
- [x] **Fase 7.3** — Flujo protegido por login: endpoints protegidos por token, datos filtrados por `OWNS_NEWS`, cuenta demo con seed, cuenta nueva vacía, redirección a `/login` sin sesión.
- [x] **Fase 8** — Evaluar link + alta de noticia por URL (`/api/news/submit-url`), dashboard con donas, scroll interno y acordeones en el detalle.
- [x] **Fase 8.2** — Borrado de noticias (`DELETE /api/news/{id}`) con limpieza de fuente huérfana, persistencia del `riskScore` evaluado al guardar, asociación automática de temas y ocultamiento de IDs técnicos en la UI.
- [x] **Fase 8.3** — Dashboard analítico: top temas por riesgo, señales de riesgo más frecuentes, evolución temporal por riesgo y mini grafo del subgrafo (endpoints `topic-risk-ranking`, `risk-signals`, `news-timeline`, `graph-summary`); se quitó el acceso rápido a alto riesgo.
