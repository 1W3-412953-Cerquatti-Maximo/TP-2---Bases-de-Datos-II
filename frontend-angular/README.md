# NexoVeraz — Frontend Angular

SPA del proyecto **NexoVeraz — App Anti Fake News** (TP-2 Bases de Datos II).

## Requisitos

- Node.js 20+ (probado con Node 24)
- npm 10+
- Backend Spring Boot corriendo en `http://localhost:8080` (ver `../backend-java/`)
- Neo4j local con el seed cargado (ver `../cypher/`)

## Instalación

```bash
cd frontend-angular
npm install
```

## Ejecutar en modo desarrollo

```bash
npm start
# o
ng serve
```

La app levanta en **http://localhost:4200**.

> El backend Spring Boot tiene CORS habilitado **solo** para `http://localhost:4200`. Si cambiás el puerto del frontend, ajustá `WebConfig.java` en el backend.

## Build de producción

```bash
npm run build
# salida en dist/frontend-angular
```

## Pantallas disponibles

| Ruta              | Descripción                                                   |
|-------------------|---------------------------------------------------------------|
| `/dashboard`      | KPIs: total noticias, distribución por riesgo, conteos        |
| `/news`           | Listado de noticias con score, nivel y temas                  |
| `/news/:id`       | Detalle + análisis on-demand + grafo de relaciones            |
| `/sources`        | Fuentes con `credibilityScore` visualizado                    |
| `/reports`        | Reportes derivados client-side (distribución, top risk, etc.) |
| `/login`          | Ingreso (email + password)                                    |
| `/register`       | Registro (usuario, nombre, email, password + confirmación)    |
| `/profile`        | Datos del usuario, logout y selector de tema (requiere login) |

Ruta raíz `/` redirige a `/dashboard`. Cualquier ruta no encontrada también.
La app es **pública**: dashboard, noticias y reportes funcionan sin login. Solo `/profile` está protegida (guard que redirige a `/login`).

## Autenticación y tema (Fase 7.2)

- **Token**: tras login/registro se guarda en `localStorage` (`nv_token`) y un interceptor lo envía como `Authorization: Bearer <token>` en cada request.
- **Sesión persistente**: al recargar, si hay token se llama a `/api/auth/me` para restaurar el usuario.
- **Tema claro/oscuro**: `ThemeService` aplica la clase `.theme-light` en `<html>` y guarda la preferencia en `localStorage` (`nv_theme`). Por defecto **dark**. Si hay sesión, el cambio se sincroniza con `PUT /api/auth/me/preferences`.

### Probar login / register

1. Backend corriendo en `localhost:8080` con Neo4j y los constraints de `cypher/01_constraints.cypher` cargados (incluye `AppUser`/`AuthSession`).
2. `npm start` → http://localhost:4200
3. Header → **Crear cuenta** → completar y enviar → redirige a `/profile`.
4. **Cerrar sesión** desde el perfil → header vuelve a mostrar Ingresar / Crear cuenta.
5. **Ingresar** con el mismo email/password.

### Probar modo claro / oscuro

- Botón **☀ Claro / ☾ Oscuro** en el header: alterna el tema al instante.
- También desde `/profile` con el selector de tarjetas (Oscuro / Claro).
- Recargar la página: la preferencia persiste (localStorage). Con sesión activa, también queda guardada en la cuenta (`themePreference`).

## Endpoints REST consumidos

| Servicio                | Endpoint backend                          |
|-------------------------|-------------------------------------------|
| `DashboardService`      | `GET /api/dashboard/summary`              |
| `NewsService.list`      | `GET /api/news`                           |
| `NewsService.getById`   | `GET /api/news/{id}`                      |
| `NewsService.analyze`   | `GET /api/news/{id}/analysis`             |
| `GraphService`          | `GET /api/graph/news/{id}`                |
| `SourceService`         | `GET /api/sources`                        |
| `TopicService`          | `GET /api/topics`                         |
| `AiService`             | `POST /api/ai/analyze-news-text`          |
| `AuthService.register`  | `POST /api/auth/register`                 |
| `AuthService.login`     | `POST /api/auth/login`                    |
| `AuthService.loadCurrentUser` | `GET /api/auth/me`                  |
| `AuthService.updateThemePreference` | `PUT /api/auth/me/preferences` |
| `AuthService.logout`    | `POST /api/auth/logout`                   |

La base URL está centralizada en `src/app/core/api.config.ts`.
El token se adjunta automáticamente vía interceptor (`core/interceptors/auth.interceptor.ts`).

## Estructura

```
src/
├── styles.scss                           Variables CSS globales + componentes base
├── index.html                            Título y meta NexoVeraz
└── app/
    ├── app.{ts,html,scss}                Bootstrap mínimo, solo <router-outlet>
    ├── app.config.ts                     provideRouter + provideHttpClient
    ├── app.routes.ts                     Routes con Shell como padre
    ├── core/
    │   ├── api.config.ts                 API_BASE_URL
    │   ├── models/                       NewsSummary, NewsDetail, Source, Topic,
    │   │                                 DashboardSummary, NewsAnalysis, RiskSignal,
    │   │                                 GraphNode, GraphEdge, GraphResponse,
    │   │                                 AiAnalyzeNewsRequest, AiAnalyzeNewsResponse
    │   └── services/                     News, Source, Topic, Dashboard, Graph, Ai
    ├── layout/
    │   └── shell.{ts,html,scss}          Sidebar + topbar + <router-outlet>
    └── pages/
        ├── dashboard/
        ├── news-list/
        ├── news-detail/                  con análisis on-demand + grafo agrupado
        ├── sources/
        └── reports/
```

## Identidad visual

- **Nombre**: NexoVeraz
- **Slogan**: *Noticias trazables. Riesgos explicables.*
- **Estética**: dashboard oscuro, tipografía técnica, indicadores tipo semáforo.

Paleta (definida como CSS variables en `src/styles.scss`):

| Variable             | Valor       |
|----------------------|-------------|
| `--color-bg`         | `#07111F`   |
| `--color-surface`    | `#0F172A`   |
| `--color-card`       | `#111C2E`   |
| `--color-border`     | `#1E293B`   |
| `--color-text`       | `#E2E8F0`   |
| `--color-muted`      | `#94A3B8`   |
| `--color-primary`    | `#14B8A6`   |
| `--color-accent`     | `#38BDF8`   |
| `--color-low`        | `#22C55E`   |
| `--color-medium`     | `#F59E0B`   |
| `--color-high`       | `#EF4444`   |

## Notas técnicas

- **Angular 21**, standalone components, signals, nuevo control flow (`@if`, `@for`).
- **HttpClient** con `provideHttpClient(withFetch())`.
- **Sin librerías de gráficos**: el grafo se renderiza como grupos de cards (`News`, `Source`, `Topic`, `Claim`, `Evidence`, `FactCheck`, `Post`, `User`) más una tabla de aristas con propiedades. Suficiente para mostrar que Neo4j conecta entidades.
- **Sin estado global complejo**: cada página tiene sus propios `signal`s.
- **Sin login, sin IA, sin DB relacional** — fuera de scope para esta fase.
