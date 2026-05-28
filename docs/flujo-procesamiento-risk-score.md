# Flujo de procesamiento y cálculo de riskScore en NexoVeraz

> Documento técnico basado en el código real del repositorio (backend Java 21 + Spring Boot + Neo4j, frontend Angular). Donde algo es conceptual, ilustrativo o pendiente, está marcado explícitamente.

---

## 1. Idea general

NexoVeraz **no determina "verdad absoluta"**. Clasifica un **nivel de riesgo de desinformación** (`riskScore` 0–100 y `riskLevel` LOW/MEDIUM/HIGH) a partir de señales objetivas.

Principios del diseño:

- **La IA estructura información** (extrae temas, claims, evidencias, verificaciones asistidas, fecha) y genera explicación textual; **no decide el score oficial**.
- **Neo4j guarda la noticia como un grafo** conectado a fuentes, temas, claims, evidencias, fact checks, posts y usuarios.
- **El backend calcula el `riskScore` oficial con reglas determinísticas y explicables** sobre ese grafo.
- El `riskScore` **oficial** sale de un único servicio (`NewsAnalysisService`), no de la IA ni de la estimación preliminar de "Evaluar Link".

> Nota de nombres: en el repositorio **no existe** una clase `RiskScoringService` ni `NewsEnrichmentPersistenceService`. El cálculo oficial vive en `NewsAnalysisService`/`NewsAnalysisRepository`, y la persistencia del enriquecimiento en `NewsEnrichmentRepository`.

---

## 2. Flujo completo desde que el usuario carga una noticia

```
Usuario (Evaluar Link)
   │  1. pega URL → "Buscar y evaluar" (opcional, solo preview)
   ▼
POST /api/news/evaluate-link ──► NewsLinkEvaluationService.evaluateLink()
   │     · WebArticleExtractionService.extract()  (cuerpo + preview)
   │     · PreliminaryCredibilityService.diagnose() (ESTIMACIÓN PRELIMINAR, no oficial)
   │     ◄ EvaluateLinkResponse (title, contentPreview, content, credibilityDiagnosis, warnings)
   │
   │  2. "Guardar noticia"
   ▼
POST /api/news/submit-url ──► NewsController.submitUrl()
   └─► NewsProcessingPipelineService.processSubmittedUrl(userId, request)
         a. NewsService.createFromUrl()                → crea News básica (score neutro)
            └─ WebArticleExtractionService.extractMetadata() → title, cuerpo completo (analysisContent), publishedAt (METADATA)
         b. doEnrich() → AiNewsEnrichmentService.enrich()  → Anthropic estructurado (si AI on)
            └─ NewsEnrichmentRepository.persist()        → Topics/Claims/Evidence/FactCheck en Neo4j
         c. recalcRisk() → NewsAnalysisService.analyze() → riskScore OFICIAL + persiste
         ◄ SubmitNewsUrlResponse (newsId, riskScore, riskLevel, aiEnrichmentStatus, counts, warnings)
   │
   ▼
Frontend muestra el resultado oficial → /news y /news/:id leen News.riskScore persistido
   · GET /api/news/{id}        → NewsService.getById()    → NewsDetailDto
   · GET /api/graph/news/{id}  → GraphService.getNewsGraph → grafo de relaciones
```

Pasos y clases reales:

| # | Paso | Clase / método real |
|---|------|---------------------|
| 1 | Usuario pega URL y evalúa (preview opcional) | `NewsController.evaluateLink` → `NewsLinkEvaluationService.evaluateLink` |
| 2 | Frontend guarda la noticia | `news.service.ts → submitNewsUrl()` → `POST /api/news/submit-url` |
| 3 | Extraer metadata + cuerpo | `WebArticleExtractionService.extractMetadata()` |
| 4 | Crear News básica | `NewsService.createFromUrl()` → `NewsRepository.createUserSubmittedNews()` |
| 5 | Enriquecer con IA (si corresponde) | `NewsProcessingPipelineService.doEnrich()` → `AiNewsEnrichmentService.enrich()` |
| 6 | IA extrae fecha/temas/claims/evidencias/fact checks | `AiNewsEnrichmentService` + `AnthropicClient.call()` |
| 7 | Validar respuesta IA (JSON) | `AiNewsEnrichmentService.parseJson()` + `extractTopics/Claims/Evidences/FactChecks()` |
| 8 | Persistir nodos/relaciones | `NewsEnrichmentRepository.persist()` |
| 9 | Calcular riskScore oficial | `NewsProcessingPipelineService.recalcRisk()` → `NewsAnalysisService.analyze()` |
| 10 | Guardar riskScore/riskLevel | `NewsAnalysisRepository.persistRisk()` |
| 11 | Mostrar resultado | `evaluate-link.ts` (save success) |
| 12 | Detalle y grafo consultan lo persistido | `NewsRepository.findById()`, `GraphRepository.getNewsGraph()` |

---

## 3. Servicio central de procesamiento — `NewsProcessingPipelineService`

**Por qué existe:** antes había caminos separados (crear noticia, enriquecer, calcular riesgo) con lógica duplicada e inconsistente. El pipeline es el **flujo oficial único** que orquesta los servicios existentes sin duplicar lógica de IA, persistencia ni riesgo.

**Endpoints que lo usan:**
- `POST /api/news/submit-url` → `processSubmittedUrl(userId, request)`.

> `processExistingNews(userId, newsId)` existe como capacidad interna de reproceso, pero **ya no está expuesta por HTTP**: el endpoint manual `POST /api/news/{id}/ai-enrichment` fue **eliminado** (la UI manual de enriquecimiento se retiró). El enriquecimiento corre dentro del pipeline al guardar.

**Servicios que coordina (inyectados):** `NewsService`, `AiNewsEnrichmentService`, `NewsAnalysisService`, `NewsRepository`. Lee `ai.enabled` y `ai.provider`.

**Métodos reales:**
- `SubmitNewsUrlResponse processSubmittedUrl(String userId, SubmitNewsUrlRequest request)`
- `AiNewsEnrichmentResponse processExistingNews(String userId, String newsId)` (interno)
- helpers privados: `doEnrich(...)` (best-effort, nunca pierde la noticia), `recalcRisk(...)` (servicio oficial), `aiAvailable()`.

**`NewsProcessingOptions`** (record real):

| Campo | Significado |
|-------|-------------|
| `enrichWithAi` | ejecutar enriquecimiento IA |
| `calculateRisk` | recalcular riskScore oficial |
| `persist` | persistir en Neo4j (false = preview) |
| `replacePreviousAiEnrichment` | reemplazar datos AI_ENRICHMENT previos |

Fábricas: `NewsProcessingOptions.forSubmittedUrl(aiAvailable)` = `(aiAvailable, true, true, false)`; `forReprocess()` = `(true, true, true, true)`.

**`NewsProcessingResult`** (record real, representación unificada interna): `newsId, title, url, sourceName, publishedAt, riskScore, riskLevel, aiEnrichmentStatus, topicsCount, claimsCount, evidencesCount, factChecksCount, warnings, created, updated`.

> El endpoint `submit-url` no devuelve `NewsProcessingResult` directamente: mapea a **`SubmitNewsUrlResponse`** (ver §11) por compatibilidad con el frontend.

**Regla de riesgo en el pipeline (actual, post Subfase C):** `processSubmittedUrl` **siempre** llama a `recalcRisk()` si `calculateRisk` (ya no hay condición "solo si hay claims"). Si el cálculo falla, queda el default neutro + warning, nunca el score del frontend.

---

## 4. Rol de la IA

La IA cumple **dos roles distintos**, ambos sobre Anthropic real vía `AnthropicClient` (HttpClient nativo de Java 21, sin SDK):

### 4.1 Enriquecimiento estructurado — `AiNewsEnrichmentService.enrich()`
Extrae JSON estructurado del **texto disponible** (no navega internet, no hace fact-checking externo real):

- `publishedAt`, `topics`, `claims`, `evidences`, `factChecks`, `warnings`.
- **Temas limitados al catálogo existente**: obtiene los `:Topic` reales con `TopicRepository.findAllNames()`, los pasa al prompt como "temas permitidos" y descarta los inventados (`extractTopics()` filtra contra el catálogo; fallback a "General" solo si existe). La persistencia usa `MATCH` (no crea Topics nuevos).
- Validación: `parseJson()` limpia fences ```json``` y recorta a `{...}`; clamps de `confidence` 0–1 y `aiRiskScore` 0–100; `riskLevel`∈{LOW,MEDIUM,HIGH}; `kind`/`verdict` validados contra sets; máximo 5 por lista.
- DTOs internos: `AiExtractedTopic`, `AiExtractedClaim`, `AiExtractedEvidence`, `AiExtractedFactCheck`. Respuesta: `AiNewsEnrichmentResponse`.
- Todo lo persistido queda marcado: `origin = 'AI_ENRICHMENT'`, y en relaciones/nodos según corresponda `source`/`provider`/`model`/`confidence`/`generatedAt`/`extractionMethod = 'ANTHROPIC'`.

### 4.2 Asistente IA (explicación textual) — `AiAnalysisPort` / `AnthropicAiAnalysisService`
`POST /api/ai/analyze-news-text` → `aiAnalysisPort.analyze()`. Devuelve `AiAnalyzeNewsResponse`: `summary`, `riskAnalysis`, `aiRiskLevel`, `aiRiskScore`, `warningSignals`, `recommendations`, `limitations`, `confidence`, `usage`. **Es solo explicativo: no toca `News.riskScore`** (ver §10).

Selección de implementación de `AiAnalysisPort` por `ai.provider` (en `AiConfig`): `DisabledAiAnalysisService` / `MockAiAnalysisService` / `AnthropicAiAnalysisService` / `ExternalAiAnalysisService` (placeholder).

### 4.3 Diagnóstico de conexión — `AiProvider` / `AiService`
`AnthropicAiProvider` (real) y `MockAiProvider` implementan `AiProvider.testConnection()`; `AiService.health()/test()` alimentan `GET /api/ai/health` y `POST /api/ai/test` (`AiTestResponse`).

> `web_fetch` de Anthropic **no está implementado**: por diseño, el backend extrae el contenido y se lo envía a Claude (control y trazabilidad).

---

## 5. Modelo de grafo en Neo4j

Nodos (etiquetas reales; restricciones de unicidad de `id` en `cypher/01_constraints.cypher`):

| Nodo | Representa | Propiedades principales | ¿Puede venir de IA? | ¿Afecta riskScore? |
|------|-----------|--------------------------|---------------------|--------------------|
| `AppUser` | Usuario autenticado de NexoVeraz | `id`, credenciales/token | No | No (define propiedad de la noticia) |
| `News` | Noticia analizada | `id, title, content, url, createdAt, publishedAt, publishedAtSource, publishedAtConfidence, status, riskScore, riskLevel, origin`, props de IA (§8) | Se crea por carga; props IA se agregan | Sí (es el sujeto) |
| `Source` | Medio/origen | `id, name, type, url, credibilityScore, country, domain` | No (se crea al guardar URL) | **Sí** (credibilidad < 0.4) |
| `Topic` | Categoría temática | `id, name, slug, origin` | Se **vincula** desde IA, pero **no se crean nuevos** por IA | No directamente |
| `Claim` | Afirmación detectada | `id, text, status`, +IA: `type, riskLevel, confidence, explanation, origin, createdAt` | Sí (`origin='AI_ENRICHMENT'`) | **Sí** (refutados / sin evidencia) |
| `Evidence` | Señal de evidencia | `id, description, type, url`, +IA: `kind, confidence, explanation, origin, createdAt` | Sí | **Sí** (vía relación con Claim) |
| `FactCheck` | Verificación (asistida si es IA) | `id, verdict, explanation, confidence, publishedAt`, +IA: `origin, provider, model, checkedAt` | Sí | **Sí** (FALSE/MISLEADING) |
| `Post` | Publicación que difunde | `id, content, platform, createdAt` | No (seed/simulación) | **Sí** (volumen/alcance) |
| `User` | Usuario ficticio de circulación | `id, username, role` | No (seed/simulación) | **Sí** (usuarios conectados) |

Relaciones **implementadas** (creadas y/o leídas por el código):

```
(AppUser)-[:OWNS_NEWS]->(News)                     // pertenencia (filtro de todo)
(News)-[:PUBLISHED_BY]->(Source)
(News)-[:ABOUT {relevance, confidence, source, generatedAt}]->(Topic)
(News)-[:CONTAINS {extractedAt, extractionMethod, confidence}]->(Claim)
(Claim)-[:SUPPORTED_BY]->(Evidence)
(Claim)-[:REFUTED_BY]->(Evidence)
(Claim)-[:HAS_EVIDENCE_GAP]->(Evidence)            // gaps generados por IA
(FactCheck)-[:CHECKS]->(Claim)
(Post)-[:SPREADS]->(News)
(User)-[:CREATED]->(Post) , (User)-[:SHARED]->(Post)
(User)-[:FOLLOWS|INTERACTS_WITH]-(User)            // usado en señal de usuarios conectados
```

Relación **pendiente / conceptual** (la query del grafo la lee, pero el enriquecimiento IA actual **no la genera**):

```
(FactCheck)-[:BASED_ON]->(Evidence)   // PENDIENTE: leída por GraphRepository, no creada por la IA hoy
```

---

## 6. Cómo se calcula el `riskScore` (oficial)

**Clase:** `NewsAnalysisService`. **Método:** `analyze(String userId, String id)`. **Datos:** `NewsAnalysisRepository.fetchSignals(userId, id)` ejecuta UNA consulta Cypher que recorre el grafo de la noticia y devuelve indicadores numéricos (`AnalysisInputs`): credibilidad de fuente, conteos de fact checks FALSE/MISLEADING, claims refutados, claims sin evidencia, cantidad de posts, alcance máximo y pares de usuarios conectados.

El score es la **suma de pesos fijos** (hardcodeados como constantes en `NewsAnalysisService`), con tope 100:

| Señal (código `RiskSignalDto`) | Condición real | Puntos |
|--------------------------------|----------------|--------|
| `LOW_CREDIBILITY_SOURCE` | `source.credibilityScore < 0.4` | **+25** |
| `FALSE_FACT_CHECK` | ≥1 FactCheck con `verdict='FALSE'` sobre un claim | **+40** |
| `MISLEADING_FACT_CHECK` | ≥1 FactCheck `verdict='MISLEADING'` | **+25** |
| `CLAIM_REFUTED_BY_EVIDENCE` | ≥1 Claim con `[:REFUTED_BY]->(:Evidence)` | **+30** |
| `CLAIM_WITHOUT_EVIDENCE` | ≥1 Claim **sin** `[:SUPPORTED_BY\|REFUTED_BY]->(:Evidence)` | **+20** |
| `HIGH_PROPAGATION_VOLUME` | más de 3 Posts que difunden la noticia | **+10** |
| `CONNECTED_USERS_PROPAGATION` | usuarios que la difundieron están conectados (`FOLLOWS`/`INTERACTS_WITH`) | **+15** |
| `HIGH_REACH_POST` | algún Post con `reach > 20000` | **+10** |

- **No hay factores que "reduzcan" el riesgo** (no hay puntajes negativos). La evidencia favorable (`SUPPORTED_BY`) reduce el riesgo solo de forma **indirecta**: hace que el claim **deje de contar** como `CLAIM_WITHOUT_EVIDENCE` (evita el +20).
- Detalle importante: un claim con **solo** `HAS_EVIDENCE_GAP` (no `SUPPORTED_BY`/`REFUTED_BY`) **sí** cuenta como `CLAIM_WITHOUT_EVIDENCE` (+20).
- Normalización: `finalScore = min(suma, 100)`.
- Constantes reales: `LOW_CREDIBILITY_THRESHOLD=0.4`, `POST_VOLUME_THRESHOLD=3`, `HIGH_REACH_THRESHOLD=20000`, `MAX_SCORE=100`, `MEDIUM_LEVEL_MIN=40`, `HIGH_LEVEL_MIN=70`.

```
riskScore OFICIAL = min(100, Σ pesos de señales detectadas en el grafo)
```

**No confundir** con `aiRiskScore` (Asistente IA), la estimación preliminar de Evaluar Link, ni el análisis textual de la IA.

---

## 7. RiskLevel

`NewsAnalysisService.computeLevel(score)` (umbrales reales del código):

| Rango de `riskScore` | `riskLevel` |
|----------------------|-------------|
| `>= 70` | HIGH |
| `40–69` (`>= 40`) | MEDIUM |
| `0–39` | LOW |

(Coincide con el ejemplo 0–39 LOW / 40–69 MEDIUM / 70–100 HIGH.)

---

## 8. Persistencia del resultado

`NewsAnalysisRepository.persistRisk(id, score, level)` escribe en `:News` (fuente única del score oficial):

```cypher
MATCH (n:News {id: $id})
SET n.riskScore = $score,
    n.riskLevel = $level,
    n.status = 'ANALYZED',
    n.riskCalculatedAt = datetime(),
    n.riskCalculationSource = 'GRAPH_RULES',
    n.riskCalculationVersion = 'v1'
```

Otras propiedades reales de `:News` relacionadas:

- Enriquecimiento IA (en `NewsEnrichmentRepository.persist`): `aiEnrichedAt`, `aiEnrichmentStatus` (`'COMPLETED'`/`'FAILED'`), `aiEnrichmentProvider`, `aiEnrichmentModel`, `aiEnrichmentError` (si falló).
- Fechas: `createdAt`, `publishedAt`, `publishedAtSource`, `publishedAtConfidence` (§9).

**Consistencia:** Dashboard (`/api/dashboard/*`), Noticias (`/api/news`) y Detalle (`/api/news/{id}`) leen **el mismo `News.riskScore` persistido**; no recalculan por su cuenta. La estimación preliminar de Evaluar Link **no** se persiste como oficial.

---

## 9. Fecha de carga vs fecha de publicación

| Propiedad | Significado | Cómo se setea |
|-----------|-------------|---------------|
| `createdAt` | cuándo se cargó la noticia en NexoVeraz | `datetime()` al crear; **nunca** se sobrescribe |
| `publishedAt` | cuándo fue publicada la noticia | metadata al crear, o IA al enriquecer; puede quedar `null` |
| `publishedAtSource` | origen de la fecha | `'METADATA'` (detectada al crear), `'AI_INFERRED'` (IA), `'UNKNOWN'` (no detectada) |
| `publishedAtConfidence` | confianza si fue inferida | `1.0` para METADATA; valor del modelo para AI_INFERRED; `null` si no hay |

- Detección de fecha por metadata: `WebArticleExtractionService.detectPublishedAt()` (JSON-LD `datePublished`, OG `article:published_time`/`og:published_time`, meta `pubdate`/`publishdate`/`date`/`DC.date`/`DC.date.issued`/`itemprop=datePublished`, `time[datetime]`), normalizada con `normalizeIsoDate()`.
- Actualización por IA (`NewsEnrichmentRepository.persist`): solo si `publishedAt` está `null` **o** parece la fecha de carga (`publishedAt = createdAt`) **y** la confianza IA es `>= 0.6`.
- Valores de `publishedAtSource` **definidos como posibles pero no usados hoy** por el código: `SCRAPED`, `USER_PROVIDED` (pendientes).

**Qué fecha se muestra:**
- Tabla Noticias (`NewsSummaryDto` tiene `publishedAt` y `createdAt`): muestra `publishedAt`; si es null, **fallback a `createdAt`**. Orden: `coalesce(publishedAt, createdAt) DESC`.
- Detalle (`NewsDetailDto`): muestra **"Fecha de publicación"** (`publishedAt`, con un discreto "· estimada por IA" si `publishedAtSource='AI_INFERRED'`) y **"Cargada en NexoVeraz"** (`createdAt`).

---

## 10. `riskScore` oficial vs Asistente IA

| | Asistente IA (`AiAnalyzeNewsResponse`) | riskScore oficial (`News.riskScore`) |
|---|----------------------------------------|--------------------------------------|
| Origen | `AnthropicAiAnalysisService` (texto) | `NewsAnalysisService` (reglas sobre grafo) |
| Contenido | summary, riskAnalysis, `aiRiskLevel`, `aiRiskScore`, warningSignals, recommendations, limitations, confidence | número 0–100 + LOW/MEDIUM/HIGH |
| ¿Persiste en `News`? | **No** | **Sí** (`persistRisk`) |
| ¿Es la verdad final? | No, es explicativo | Es el score oficial del sistema |

El `aiRiskScore` del Asistente **no** reemplaza ni modifica `News.riskScore`.

---

## 11. Flujo de endpoints

| Endpoint | Recibe | Controller | Servicio | Devuelve | ¿Persiste? |
|----------|--------|-----------|----------|----------|-----------|
| `POST /api/news/evaluate-link` | `EvaluateLinkRequest {url}` | `NewsController.evaluateLink` | `NewsLinkEvaluationService.evaluateLink` | `EvaluateLinkResponse` (preview + diagnóstico **preliminar**) | No |
| `POST /api/news/submit-url` | `SubmitNewsUrlRequest` | `NewsController.submitUrl` | `NewsProcessingPipelineService.processSubmittedUrl` | `SubmitNewsUrlResponse` | **Sí** (News + IA + risk) |
| `GET /api/news/{id}/analysis` | path id | `NewsController.analyze` | `NewsAnalysisService.analyze` | `NewsAnalysisDto` | **Sí** (riskScore oficial) |
| `GET /api/news` | — | `NewsController.listAll` | `NewsService.listAll` | `List<NewsSummaryDto>` | No |
| `GET /api/news/{id}` | path id | `NewsController.getById` | `NewsService.getById` | `NewsDetailDto` | No |
| `DELETE /api/news/{id}` | path id | `NewsController.delete` | `NewsService.deleteNews` | `DeleteNewsResponse` | Sí (borra) |
| `GET /api/graph/news/{id}` | path id | `GraphController.getNewsGraph` | `GraphService.getNewsGraph` | `GraphResponseDto` (nodes+edges) | No |
| `POST /api/ai/analyze-news-text` | `AiAnalyzeNewsRequest` | `AiController.analyzeNewsText` | `AiAnalysisPort.analyze` | `AiAnalyzeNewsResponse` | No |
| `GET /api/ai/health` | — | `AiController.health` | `AiService.health` | `AiTestResponse` | No |
| `POST /api/ai/test` | `AiTestRequest` (opcional) | `AiController.test` | `AiService.test` | `AiTestResponse` | No |
| `GET /api/dashboard/summary` | — | `DashboardController.getSummary` | `DashboardService.getSummary` | `DashboardSummaryDto` (usa `riskScore`/`riskLevel`) | No |
| `GET /api/dashboard/topic-risk-ranking` `/risk-signals` `/news-timeline` `/graph-summary` | — | `DashboardController.*` | `DashboardService.*` | DTOs de dashboard | No |

> Todos exigen Bearer token vía `AuthenticatedUserResolver.requireUserId()/requireCurrentUser()`. El endpoint manual `POST /api/news/{id}/ai-enrichment` **fue eliminado** (Subfase D).

---

## 12. Ejemplo completo (ilustrativo)

Noticia: **"Viralizan que un edulcorante común provoca cáncer seguro"** (texto que afirma de forma absoluta sin citar estudios ni organismos).

Lo que el pipeline produce (los **textos** son ilustrativos; los **pesos** del score son reales):

- **Fuente detectada:** medio web por dominio → `Source {credibilityScore: 0.5}` (default al guardar por URL).
- **Tema:** `Salud` y/o `Consumo y Alimentos` (si están en el catálogo de `:Topic`; si no, se ignora o "General").
- **Claims (IA):** `"El edulcorante provoca cáncer de forma segura"` (`riskLevel: HIGH`, `origin: AI_ENRICHMENT`).
- **Evidencias (IA):** `"No cita estudios clínicos ni organismos sanitarios"` (`kind: MISSING_EVIDENCE` → relación `HAS_EVIDENCE_GAP`).
- **FactChecks (IA):** `verdict: REQUIRES_VERIFICATION` (asistido, no externo).
- **Relaciones en Neo4j:**
  ```
  (AppUser)-[:OWNS_NEWS]->(News)-[:PUBLISHED_BY]->(Source)
  (News)-[:ABOUT]->(Topic:Salud)
  (News)-[:CONTAINS]->(Claim {riskLevel:HIGH})-[:HAS_EVIDENCE_GAP]->(Evidence {kind:MISSING_EVIDENCE})
  (FactCheck {verdict:REQUIRES_VERIFICATION})-[:CHECKS]->(Claim)
  ```
- **Señales de riesgo OFICIALES que disparan** (reglas reales):
  - `LOW_CREDIBILITY_SOURCE`: **no** (0.5 ≥ 0.4) → +0
  - `FALSE`/`MISLEADING_FACT_CHECK`: **no** (verdict REQUIRES_VERIFICATION) → +0
  - `CLAIM_REFUTED_BY_EVIDENCE`: **no** → +0
  - `CLAIM_WITHOUT_EVIDENCE`: **sí** (el claim solo tiene `HAS_EVIDENCE_GAP`) → **+20**
  - propagación/alcance/usuarios conectados: **no** (noticia recién cargada, sin Posts) → +0
- **Cálculo conceptual:** `riskScore = min(100, 20) = 20` → **riskLevel = LOW**.

> Observación honesta: aunque el texto "suena" muy riesgoso, el score **oficial** es bajo porque las reglas determinísticas se apoyan en señales del grafo (fact checks FALSE/MISLEADING, claims refutados, fuente de baja credibilidad, propagación) que una noticia recién cargada todavía no tiene. La estimación **preliminar** de Evaluar Link (heurística de texto) o el `aiRiskScore` del Asistente podrían ser más altos, pero **no son** el score oficial. Ver §14.

---

## 13. Por qué Neo4j es útil

- Permite **recorrer relaciones** (noticia → claims → evidencias → fact checks) en una sola consulta (`fetchSignals`).
- Analiza **contexto y propagación**: posts que difunden, usuarios conectados entre sí, alcance.
- Conecta la noticia con **fuente, temas, claims y evidencias** en vez de tratarla como un registro aislado.
- Habilita el **grafo de relaciones** visual (`GraphRepository.getNewsGraph`) y métricas agregadas del Dashboard.
- Las reglas de riesgo se expresan naturalmente como **patrones de grafo** (ej. `NOT EXISTS { (c)-[:SUPPORTED_BY|REFUTED_BY]->(:Evidence) }`).

---

## 14. Limitaciones actuales

- La IA **no verifica internet en tiempo real**; analiza solo el texto extraído por el backend.
- Los **fact checks de IA son asistidos** (`verdict` orientativo, a menudo `REQUIRES_VERIFICATION`/`UNVERIFIED`), **no** verificación externa definitiva.
- `Post`/`User` reales solo existen por **seed/simulación**; el pipeline IA **no** crea Posts/Usuarios.
- Si faltan datos estructurados (noticia recién cargada, sin fact checks ni propagación), el score oficial tiende a ser **bajo aunque el contenido sea sospechoso** — el sistema mide señales de grafo, no "verdad".
- `(FactCheck)-[:BASED_ON]->(Evidence)` está **pendiente** (la query del grafo la contempla, pero no se genera hoy).
- `publishedAtSource` `SCRAPED`/`USER_PROVIDED` están definidos como posibles pero **no se setean** actualmente.
- El sistema **estima riesgo de desinformación**, no falsedad/verdad absoluta.

---

## 15. Resumen ejecutivo

> **La IA extrae los componentes (temas, claims, evidencias, verificaciones asistidas, fecha), Neo4j los conecta como un grafo, y el backend calcula un `riskScore` oficial con reglas determinísticas y explicables sobre esas relaciones.** La IA estructura; Neo4j contextualiza; las reglas deciden el score. El resultado se persiste una sola vez en `News` y todas las vistas (Noticias, Detalle, Dashboard) leen ese mismo valor.
