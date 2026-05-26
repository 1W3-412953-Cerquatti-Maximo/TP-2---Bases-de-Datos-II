// ============================================================
// 04_additional_news_seed.cypher
// Seed ADICIONAL de noticias — NexoVeraz (App Anti Fake News)
// TP-2 Bases de Datos II
//
// Agrega VOLUMEN y VARIEDAD: 22 noticias nuevas (news-009..news-030)
// con buena distribución de riesgo (7 LOW, 9 MEDIUM, 6 HIGH), más
// fuentes, temas, claims, evidencias, fact checks, posts y usuarios.
//
// Convenciones (idénticas a 02_seed_data.cypher):
//   * riskScore ∈ [0, 100]   — LOW 0..39 · MEDIUM 40..69 · HIGH 70..100
//   * credibilityScore ∈ [0, 1] — Alta >=0.7 · Media 0.4..0.7 · Baja <0.4
//   * confidence/relevance/weight/interactionStrength ∈ [0, 1]
//
// CARACTERÍSTICAS:
//   * ADITIVO: no borra nada (sin MATCH (n) DETACH DELETE n).
//   * IDEMPOTENTE: usa MERGE por id + SET; re-ejecutar no duplica.
//   * Ejecutar DESPUÉS de 01_constraints.cypher y 02_seed_data.cypher.
//   * Todos los datos son FICTICIOS (no hay personas ni medios reales).
//
// CUENTA DEMO:
//   La sección final asocia TODAS las noticias nuevas a la cuenta demo
//   (demo@nexoveraz.local) vía OWNS_NEWS. Esa cuenta la crea
//   DemoDataInitializer al arrancar el backend (no se crea acá para no
//   manejar passwordHash en Cypher).
//
//   >>> Si la cuenta demo NO existe todavía, la última sección no vinculará
//   >>> nada (MATCH vacío). En ese caso: levantar el backend una vez para que
//   >>> DemoDataInitializer cree la cuenta demo y enlace las noticias sin dueño,
//   >>> o re-ejecutar este script luego de que la cuenta exista. El script es
//   >>> idempotente, así que re-correrlo es seguro.
// ============================================================


// ============================================================
// 1. SOURCES (8 nuevas) — credibilidad variada
// ============================================================

MERGE (s:Source {id: 'src-009'}) SET s.name='Agencia Central de Noticias',     s.url='https://agenciacentral.example',  s.credibilityScore=0.90, s.type='news_agency';
MERGE (s:Source {id: 'src-010'}) SET s.name='Instituto de Divulgación Científica', s.url='https://divulgaciencia.example', s.credibilityScore=0.86, s.type='science_outlet';
MERGE (s:Source {id: 'src-011'}) SET s.name='Observatorio Económico',          s.url='https://observatorioeco.example', s.credibilityScore=0.72, s.type='digital_news';
MERGE (s:Source {id: 'src-012'}) SET s.name='Portal Metrópoli',                s.url='https://portalmetropoli.example', s.credibilityScore=0.60, s.type='digital_news';
MERGE (s:Source {id: 'src-013'}) SET s.name='La Voz Regional',                 s.url='https://lavozregional.example',   s.credibilityScore=0.52, s.type='newspaper';
MERGE (s:Source {id: 'src-014'}) SET s.name='Tendencias Diarias',              s.url='https://tendenciasdiarias.example', s.credibilityScore=0.44, s.type='tabloid';
MERGE (s:Source {id: 'src-015'}) SET s.name='Alerta Viral 24',                 s.url='https://alertaviral24.example',   s.credibilityScore=0.30, s.type='clickbait';
MERGE (s:Source {id: 'src-016'}) SET s.name='MisterioGlobal',                  s.url='https://misterioglobal.example',  s.credibilityScore=0.14, s.type='conspiracy_blog';


// ============================================================
// 2. TOPICS (6 nuevos) — se reutilizan también topic-001..006
// ============================================================

MERGE (t:Topic {id: 'topic-007'}) SET t.name='Seguridad',           t.slug='seguridad';
MERGE (t:Topic {id: 'topic-008'}) SET t.name='Educación',           t.slug='educacion';
MERGE (t:Topic {id: 'topic-009'}) SET t.name='Transporte',          t.slug='transporte';
MERGE (t:Topic {id: 'topic-010'}) SET t.name='Energía',             t.slug='energia';
MERGE (t:Topic {id: 'topic-011'}) SET t.name='Consumo y Alimentos', t.slug='consumo-alimentos';
MERGE (t:Topic {id: 'topic-012'}) SET t.name='Medio Ambiente',      t.slug='medio-ambiente';


// ============================================================
// 3. NEWS (22) — news-009..news-030
//    LOW (7): news-009..news-015
//    MEDIUM (9): news-016..news-024
//    HIGH (6): news-025..news-030
// ============================================================

// --- LOW (fuentes confiables, con evidencia) ---

MERGE (n:News {id:'news-009'})
SET n.title='Salud amplía el calendario de vacunación infantil',
    n.content='El Ministerio de Salud incorporó dos vacunas al esquema obligatorio tras la recomendación del comité de inmunizaciones.',
    n.publishedAt=datetime('2026-04-01T09:00:00'),
    n.url='https://agenciacentral.example/salud/calendario-vacunacion',
    n.riskScore=12, n.riskLevel='LOW';

MERGE (n:News {id:'news-010'})
SET n.title='Programa de alfabetización digital llega a escuelas rurales',
    n.content='Una iniciativa pública entregó equipamiento y capacitación docente en 1.200 escuelas rurales durante el último año.',
    n.publishedAt=datetime('2026-04-02T10:30:00'),
    n.url='https://agenciacentral.example/educacion/alfabetizacion-digital',
    n.riskScore=14, n.riskLevel='LOW';

MERGE (n:News {id:'news-011'})
SET n.title='Equipo local desarrolla una batería de litio más duradera',
    n.content='Un grupo de investigación publicó en una revista indexada un prototipo de batería con mayor cantidad de ciclos de carga.',
    n.publishedAt=datetime('2026-04-03T11:15:00'),
    n.url='https://divulgaciencia.example/tecnologia/bateria-litio',
    n.riskScore=18, n.riskLevel='LOW';

MERGE (n:News {id:'news-012'})
SET n.title='El banco central mantiene la tasa de interés tras su reunión mensual',
    n.content='La autoridad monetaria comunicó que la tasa de referencia permanece sin cambios, en línea con lo esperado por el mercado.',
    n.publishedAt=datetime('2026-04-04T16:00:00'),
    n.url='https://observatorioeco.example/economia/tasa-interes',
    n.riskScore=20, n.riskLevel='LOW';

MERGE (n:News {id:'news-013'})
SET n.title='Una nueva línea de colectivos eléctricos entra en servicio en el centro',
    n.content='La autoridad de transporte habilitó el corredor central con unidades eléctricas y paradas accesibles.',
    n.publishedAt=datetime('2026-04-05T08:45:00'),
    n.url='https://agenciacentral.example/transporte/colectivos-electricos',
    n.riskScore=16, n.riskLevel='LOW';

MERGE (n:News {id:'news-014'})
SET n.title='El parque solar regional comienza a aportar energía a la red',
    n.content='La primera etapa del parque fotovoltaico inyecta energía equivalente al consumo de unos 30.000 hogares.',
    n.publishedAt=datetime('2026-04-06T12:00:00'),
    n.url='https://observatorioeco.example/energia/parque-solar',
    n.riskScore=22, n.riskLevel='LOW';

MERGE (n:News {id:'news-015'})
SET n.title='La reforestación urbana suma 5.000 árboles nativos en un año',
    n.content='El plan municipal de arbolado completó su primera etapa con especies nativas en plazas y veredas.',
    n.publishedAt=datetime('2026-04-07T09:30:00'),
    n.url='https://divulgaciencia.example/ambiente/reforestacion-urbana',
    n.riskScore=19, n.riskLevel='LOW';

// --- MEDIUM (lenguaje alarmista, fuente media o evidencia incompleta) ---

MERGE (n:News {id:'news-016'})
SET n.title='Advierten por un posible faltante de un medicamento común en farmacias',
    n.content='Algunas farmacias reportan demoras en la reposición de un antifebril de uso frecuente; aún no hay datos consolidados.',
    n.publishedAt=datetime('2026-04-08T18:20:00'),
    n.url='https://portalmetropoli.example/salud/faltante-medicamento',
    n.riskScore=48, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-017'})
SET n.title='Analistas anticipan un "fuerte salto" del dólar para fin de mes',
    n.content='El título generaliza una proyección de un único informe que contemplaba un escenario extremo entre varios posibles.',
    n.publishedAt=datetime('2026-04-09T20:10:00'),
    n.url='https://tendenciasdiarias.example/economia/salto-dolar',
    n.riskScore=58, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-018'})
SET n.title='La oposición denuncia una "maniobra" en la sesión del congreso',
    n.content='Legisladores opositores cuestionaron el procedimiento de votación; el oficialismo lo niega y no hay aún dictamen oficial.',
    n.publishedAt=datetime('2026-04-10T19:40:00'),
    n.url='https://lavozregional.example/politica/maniobra-sesion',
    n.riskScore=55, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-019'})
SET n.title='Vecinos reportan una ola de robos en el barrio, según fuentes locales',
    n.content='Comerciantes describen un aumento de hechos en las últimas semanas; las estadísticas oficiales todavía no lo confirman.',
    n.publishedAt=datetime('2026-04-11T21:00:00'),
    n.url='https://lavozregional.example/seguridad/ola-robos',
    n.riskScore=50, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-020'})
SET n.title='Dudas sobre la calidad de un aceite de marca popular',
    n.content='Una publicación viral cuestiona un aceite comercial a partir de un análisis cuyo origen y metodología no están claros.',
    n.publishedAt=datetime('2026-04-12T17:30:00'),
    n.url='https://tendenciasdiarias.example/consumo/aceite-calidad',
    n.riskScore=52, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-021'})
SET n.title='Pronostican un verano "extremo" con temperaturas inéditas',
    n.content='Una nota amplifica un escenario de máxima de un modelo climático, presentándolo como pronóstico confirmado.',
    n.publishedAt=datetime('2026-04-13T11:50:00'),
    n.url='https://portalmetropoli.example/clima/verano-extremo',
    n.riskScore=47, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-022'})
SET n.title='Aumentarían las tarifas del transporte un 40%, según trascendidos',
    n.content='Circula un borrador no confirmado que menciona una suba; las autoridades aún no comunicaron una cifra oficial.',
    n.publishedAt=datetime('2026-04-14T08:20:00'),
    n.url='https://tendenciasdiarias.example/transporte/tarifas-suba',
    n.riskScore=60, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-023'})
SET n.title='Una app promete "detectar enfermedades" con la cámara del celular',
    n.content='La aplicación ofrece orientación general, pero la nota la presenta como herramienta de diagnóstico médico.',
    n.publishedAt=datetime('2026-04-15T13:25:00'),
    n.url='https://portalmetropoli.example/tecnologia/app-deteccion',
    n.riskScore=56, n.riskLevel='MEDIUM';

MERGE (n:News {id:'news-024'})
SET n.title='Polémica por un cambio en el régimen de exámenes universitarios',
    n.content='Una reforma propuesta generó debate entre centros de estudiantes; el texto definitivo aún no fue publicado.',
    n.publishedAt=datetime('2026-04-16T10:05:00'),
    n.url='https://lavozregional.example/educacion/regimen-examenes',
    n.riskScore=45, n.riskLevel='MEDIUM';

// --- HIGH (fuente poco confiable, claim refutado, fact check FALSE o difusión coordinada) ---

MERGE (n:News {id:'news-025'})
SET n.title='Afirman que un té casero "cura" la diabetes en una semana',
    n.content='Un sitio sin respaldo médico asegura una cura milagrosa; sociedades científicas lo desmienten.',
    n.publishedAt=datetime('2026-04-17T22:15:00'),
    n.url='https://misterioglobal.example/salud/te-cura-diabetes',
    n.riskScore=90, n.riskLevel='HIGH';

MERGE (n:News {id:'news-026'})
SET n.title='Denuncian que los medidores inteligentes "espían" a los hogares',
    n.content='Una cadena afirma que los medidores graban conversaciones, algo que el ente regulador descarta técnicamente.',
    n.publishedAt=datetime('2026-04-18T23:00:00'),
    n.url='https://misterioglobal.example/energia/medidores-espian',
    n.riskScore=85, n.riskLevel='HIGH';

MERGE (n:News {id:'news-027'})
SET n.title='Alertan por un "corralito inminente" y llaman a retirar ahorros',
    n.content='Cuentas coordinadas difunden un supuesto bloqueo de depósitos; el banco central lo desmiente por completo.',
    n.publishedAt=datetime('2026-04-19T20:30:00'),
    n.url='https://alertaviral24.example/economia/corralito-inminente',
    n.riskScore=82, n.riskLevel='HIGH';

MERGE (n:News {id:'news-028'})
SET n.title='Una cadena viral asegura secuestros masivos coordinados por una app',
    n.content='El mensaje se replica en redes sin pruebas; la policía emitió un parte oficial desmintiendo la versión.',
    n.publishedAt=datetime('2026-04-20T21:45:00'),
    n.url='https://alertaviral24.example/seguridad/secuestros-app',
    n.riskScore=88, n.riskLevel='HIGH';

MERGE (n:News {id:'news-029'})
SET n.title='Viralizan que un edulcorante común "provoca cáncer seguro"',
    n.content='Una publicación alarmista distorsiona un informe; la agencia de seguridad alimentaria aclara que el consumo moderado es seguro.',
    n.publishedAt=datetime('2026-04-21T19:10:00'),
    n.url='https://misterioglobal.example/consumo/edulcorante-cancer',
    n.riskScore=79, n.riskLevel='HIGH';

MERGE (n:News {id:'news-030'})
SET n.title='Aseguran que el wifi del hogar "daña el ADN" de los niños',
    n.content='Un blog retoma un mito ya desmentido por revisiones científicas, sin aportar estudios revisados por pares.',
    n.publishedAt=datetime('2026-04-22T22:40:00'),
    n.url='https://alertaviral24.example/tecnologia/wifi-adn',
    n.riskScore=91, n.riskLevel='HIGH';


// ============================================================
// 4. USERS (14 nuevos) — user-020..user-033
// ============================================================

MERGE (u:User {id:'user-020'}) SET u.username='journalist_marina', u.role='JOURNALIST',   u.followerCount=15600, u.suspicious=false, u.createdAt=datetime('2025-02-12T00:00:00');
MERGE (u:User {id:'user-021'}) SET u.username='fact_checker_omar',  u.role='FACT_CHECKER', u.followerCount=6100,  u.suspicious=false, u.createdAt=datetime('2025-03-03T00:00:00');
MERGE (u:User {id:'user-022'}) SET u.username='editor_renata',      u.role='EDITOR',       u.followerCount=4800,  u.suspicious=false, u.createdAt=datetime('2025-03-28T00:00:00');
MERGE (u:User {id:'user-023'}) SET u.username='reader_tomas',       u.role='READER',       u.followerCount=210,   u.suspicious=false, u.createdAt=datetime('2025-04-18T00:00:00');
MERGE (u:User {id:'user-024'}) SET u.username='reader_valentina',   u.role='READER',       u.followerCount=175,   u.suspicious=false, u.createdAt=datetime('2025-05-22T00:00:00');
MERGE (u:User {id:'user-025'}) SET u.username='influencer_bruno',   u.role='INFLUENCER',   u.followerCount=120000,u.suspicious=false, u.createdAt=datetime('2025-06-09T00:00:00');
MERGE (u:User {id:'user-026'}) SET u.username='reader_paula',       u.role='READER',       u.followerCount=320,   u.suspicious=false, u.createdAt=datetime('2025-07-14T00:00:00');
MERGE (u:User {id:'user-027'}) SET u.username='bot_account_002',    u.role='SUSPICIOUS',   u.followerCount=38,    u.suspicious=true,  u.createdAt=datetime('2026-01-20T00:00:00');
MERGE (u:User {id:'user-028'}) SET u.username='bot_account_003',    u.role='SUSPICIOUS',   u.followerCount=51,    u.suspicious=true,  u.createdAt=datetime('2026-01-22T00:00:00');
MERGE (u:User {id:'user-029'}) SET u.username='reader_ignacio',     u.role='READER',       u.followerCount=140,   u.suspicious=false, u.createdAt=datetime('2025-08-30T00:00:00');
MERGE (u:User {id:'user-030'}) SET u.username='skeptic_marcos',     u.role='READER',       u.followerCount=2700,  u.suspicious=true,  u.createdAt=datetime('2025-09-12T00:00:00');
MERGE (u:User {id:'user-031'}) SET u.username='influencer_camila',  u.role='INFLUENCER',   u.followerCount=87000, u.suspicious=false, u.createdAt=datetime('2025-10-01T00:00:00');
MERGE (u:User {id:'user-032'}) SET u.username='reader_nicolas',     u.role='READER',       u.followerCount=95,    u.suspicious=false, u.createdAt=datetime('2025-11-05T00:00:00');
MERGE (u:User {id:'user-033'}) SET u.username='reader_agustina',    u.role='READER',       u.followerCount=430,   u.suspicious=false, u.createdAt=datetime('2025-12-18T00:00:00');


// ============================================================
// 5. POSTS (25 nuevos) — post-020..post-044
// ============================================================

MERGE (p:Post {id:'post-020'}) SET p.content='Buena noticia para la salud pública: amplían el calendario de vacunación.', p.platform='TWITTER',   p.createdAt=datetime('2026-04-01T09:30:00');
MERGE (p:Post {id:'post-021'}) SET p.content='Llega la alfabetización digital a las escuelas rurales 👏',                  p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-02T11:00:00');
MERGE (p:Post {id:'post-022'}) SET p.content='Avance local en baterías de litio, con paper publicado.',                   p.platform='TWITTER',   p.createdAt=datetime('2026-04-03T11:45:00');
MERGE (p:Post {id:'post-023'}) SET p.content='El banco central mantiene la tasa. Análisis sereno del contexto.',          p.platform='TWITTER',   p.createdAt=datetime('2026-04-04T16:30:00');
MERGE (p:Post {id:'post-024'}) SET p.content='Ya circulan los colectivos eléctricos por el centro 🚌',                     p.platform='INSTAGRAM', p.createdAt=datetime('2026-04-05T09:15:00');
MERGE (p:Post {id:'post-025'}) SET p.content='El parque solar empieza a aportar energía a la red.',                       p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-06T12:30:00');
MERGE (p:Post {id:'post-026'}) SET p.content='5.000 árboles nativos en un año, gran trabajo de arbolado urbano.',        p.platform='TWITTER',   p.createdAt=datetime('2026-04-07T10:00:00');
MERGE (p:Post {id:'post-027'}) SET p.content='¿Se viene un faltante de medicamentos? Atentos.',                          p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-08T18:45:00');
MERGE (p:Post {id:'post-028'}) SET p.content='Dicen que el dólar pega un salto fuerte a fin de mes 😰',                    p.platform='TWITTER',   p.createdAt=datetime('2026-04-09T20:30:00');
MERGE (p:Post {id:'post-029'}) SET p.content='Polémica en el congreso por la última sesión.',                            p.platform='TWITTER',   p.createdAt=datetime('2026-04-10T20:00:00');
MERGE (p:Post {id:'post-030'}) SET p.content='Vecinos preocupados por los robos en el barrio.',                          p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-11T21:30:00');
MERGE (p:Post {id:'post-031'}) SET p.content='Cuidado con este aceite, dicen que no pasa los controles.',                p.platform='TIKTOK',    p.createdAt=datetime('2026-04-12T18:00:00');
MERGE (p:Post {id:'post-032'}) SET p.content='Se viene un verano extremo, dicen 🔥',                                      p.platform='INSTAGRAM', p.createdAt=datetime('2026-04-13T12:10:00');
MERGE (p:Post {id:'post-033'}) SET p.content='40% de aumento en el transporte?? Fuente?',                                p.platform='TWITTER',   p.createdAt=datetime('2026-04-14T08:50:00');
MERGE (p:Post {id:'post-034'}) SET p.content='Una app que detecta enfermedades con la cámara, ¿en serio?',               p.platform='TWITTER',   p.createdAt=datetime('2026-04-15T13:50:00');
MERGE (p:Post {id:'post-035'}) SET p.content='Debate por el nuevo régimen de exámenes en la facultad.',                  p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-16T10:30:00');
MERGE (p:Post {id:'post-036'}) SET p.content='¡INCREÍBLE! Un té que cura la diabetes en 7 días. Compartan 🙏',           p.platform='TELEGRAM',  p.createdAt=datetime('2026-04-17T22:30:00');
MERGE (p:Post {id:'post-037'}) SET p.content='Los medidores te están espiando en tu casa. Despertá.',                    p.platform='TELEGRAM',  p.createdAt=datetime('2026-04-18T23:20:00');
MERGE (p:Post {id:'post-038'}) SET p.content='SE VIENE EL CORRALITO, saquen la plata YA.',                               p.platform='TELEGRAM',  p.createdAt=datetime('2026-04-19T20:45:00');
MERGE (p:Post {id:'post-039'}) SET p.content='URGENTE: secuestros coordinados por una app, reenvíen a todos.',           p.platform='TELEGRAM',  p.createdAt=datetime('2026-04-20T22:00:00');
MERGE (p:Post {id:'post-040'}) SET p.content='El edulcorante que tomás todos los días provoca cáncer.',                  p.platform='FACEBOOK',  p.createdAt=datetime('2026-04-21T19:30:00');
MERGE (p:Post {id:'post-041'}) SET p.content='El wifi de tu casa daña el ADN de los chicos 😱',                          p.platform='TIKTOK',    p.createdAt=datetime('2026-04-22T23:00:00');
MERGE (p:Post {id:'post-042'}) SET p.content='Confirmado el corralito, no digan que no avisamos.',                       p.platform='TWITTER',   p.createdAt=datetime('2026-04-19T21:15:00');
MERGE (p:Post {id:'post-043'}) SET p.content='Más casos de la app de secuestros, esto es real.',                         p.platform='TWITTER',   p.createdAt=datetime('2026-04-20T22:30:00');
MERGE (p:Post {id:'post-044'}) SET p.content='Probé el té y funciona, no se dejen engañar por la medicina.',             p.platform='INSTAGRAM', p.createdAt=datetime('2026-04-18T08:00:00');


// ============================================================
// 6. CLAIMS (22) — claim-009..claim-030 (uno por noticia)
// ============================================================

MERGE (c:Claim {id:'claim-009'}) SET c.text='El nuevo calendario suma dos vacunas al esquema infantil obligatorio.', c.status='VERIFIED',     c.createdAt=datetime('2026-04-01T09:30:00');
MERGE (c:Claim {id:'claim-010'}) SET c.text='El programa de alfabetización digital alcanzó 1.200 escuelas rurales.', c.status='VERIFIED',     c.createdAt=datetime('2026-04-02T11:00:00');
MERGE (c:Claim {id:'claim-011'}) SET c.text='La nueva batería de litio dura un 40% más en pruebas de laboratorio.',  c.status='VERIFIED',     c.createdAt=datetime('2026-04-03T11:45:00');
MERGE (c:Claim {id:'claim-012'}) SET c.text='El banco central mantuvo la tasa de interés sin cambios este mes.',     c.status='VERIFIED',     c.createdAt=datetime('2026-04-04T16:30:00');
MERGE (c:Claim {id:'claim-013'}) SET c.text='La nueva línea de colectivos eléctricos cubre el corredor central.',    c.status='VERIFIED',     c.createdAt=datetime('2026-04-05T09:15:00');
MERGE (c:Claim {id:'claim-014'}) SET c.text='El parque solar aporta energía equivalente a unos 30.000 hogares.',     c.status='VERIFIED',     c.createdAt=datetime('2026-04-06T12:30:00');
MERGE (c:Claim {id:'claim-015'}) SET c.text='Se plantaron 5.000 árboles nativos en el plan de reforestación urbana.', c.status='VERIFIED',    c.createdAt=datetime('2026-04-07T10:00:00');
MERGE (c:Claim {id:'claim-016'}) SET c.text='Habría faltante de un medicamento de uso común en farmacias.',          c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-08T18:45:00');
MERGE (c:Claim {id:'claim-017'}) SET c.text='El dólar dará un fuerte salto antes de fin de mes.',                    c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-09T20:30:00');
MERGE (c:Claim {id:'claim-018'}) SET c.text='Hubo una maniobra irregular en la última sesión del congreso.',         c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-10T20:00:00');
MERGE (c:Claim {id:'claim-019'}) SET c.text='Se registra una ola de robos sin precedentes en el barrio.',            c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-11T21:30:00');
MERGE (c:Claim {id:'claim-020'}) SET c.text='Un aceite de marca popular no cumpliría los estándares de calidad.',    c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-12T18:00:00');
MERGE (c:Claim {id:'claim-021'}) SET c.text='El próximo verano tendrá temperaturas nunca antes registradas.',        c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-13T12:10:00');
MERGE (c:Claim {id:'claim-022'}) SET c.text='Las tarifas de transporte aumentarán un 40% el mes que viene.',         c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-14T08:50:00');
MERGE (c:Claim {id:'claim-023'}) SET c.text='Una app detecta enfermedades usando la cámara del celular.',            c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-15T13:50:00');
MERGE (c:Claim {id:'claim-024'}) SET c.text='El nuevo régimen de exámenes elimina las mesas de diciembre.',          c.status='UNDER_REVIEW', c.createdAt=datetime('2026-04-16T10:30:00');
MERGE (c:Claim {id:'claim-025'}) SET c.text='Un té casero cura la diabetes en una semana.',                          c.status='REFUTED',      c.createdAt=datetime('2026-04-17T22:30:00');
MERGE (c:Claim {id:'claim-026'}) SET c.text='Los medidores inteligentes graban conversaciones dentro de los hogares.', c.status='REFUTED',    c.createdAt=datetime('2026-04-18T23:20:00');
MERGE (c:Claim {id:'claim-027'}) SET c.text='Habrá un corralito bancario en los próximos días.',                     c.status='REFUTED',      c.createdAt=datetime('2026-04-19T20:45:00');
MERGE (c:Claim {id:'claim-028'}) SET c.text='Existe una red que coordina secuestros masivos mediante una app.',      c.status='REFUTED',      c.createdAt=datetime('2026-04-20T22:00:00');
MERGE (c:Claim {id:'claim-029'}) SET c.text='Un edulcorante de uso masivo provoca cáncer de forma segura.',          c.status='REFUTED',      c.createdAt=datetime('2026-04-21T19:30:00');
MERGE (c:Claim {id:'claim-030'}) SET c.text='El wifi hogareño daña el ADN de los niños.',                            c.status='REFUTED',      c.createdAt=datetime('2026-04-22T23:00:00');


// ============================================================
// 7. EVIDENCE (14) — evid-009..evid-022
// ============================================================

MERGE (e:Evidence {id:'evid-009'}) SET e.description='Resolución del Ministerio de Salud sobre el nuevo calendario de vacunación', e.url='https://gov.example/salud/calendario-2026',  e.type='OFFICIAL_REPORT',        e.createdAt=datetime('2026-04-01T08:00:00');
MERGE (e:Evidence {id:'evid-010'}) SET e.description='Informe de cobertura del programa de alfabetización digital',               e.url='https://gov.example/educacion/cobertura',     e.type='OFFICIAL_REPORT',        e.createdAt=datetime('2026-04-02T09:00:00');
MERGE (e:Evidence {id:'evid-011'}) SET e.description='Paper indexado sobre durabilidad de la nueva batería de litio',            e.url='https://journal.example/litio-ciclos',        e.type='SCIENTIFIC_PUBLICATION', e.createdAt=datetime('2026-04-03T10:00:00');
MERGE (e:Evidence {id:'evid-012'}) SET e.description='Comunicado oficial del banco central sobre la tasa de interés',            e.url='https://bcentral.example/tasa-abril',         e.type='AGENCY_STATEMENT',       e.createdAt=datetime('2026-04-04T15:30:00');
MERGE (e:Evidence {id:'evid-013'}) SET e.description='Datos de generación del parque solar regional',                           e.url='https://energia.example/solar/generacion',   e.type='DATA_SERIES',            e.createdAt=datetime('2026-04-06T11:00:00');
MERGE (e:Evidence {id:'evid-014'}) SET e.description='Registro municipal del plan de reforestación urbana',                      e.url='https://municipio.example/arbolado',          e.type='OFFICIAL_REPORT',        e.createdAt=datetime('2026-04-07T09:00:00');
MERGE (e:Evidence {id:'evid-015'}) SET e.description='Posición de la sociedad de endocrinología sobre supuestas curas caseras',  e.url='https://endocrino.example/diabetes-mitos',    e.type='HEALTH_AUTHORITY',       e.createdAt=datetime('2026-04-17T20:00:00');
MERGE (e:Evidence {id:'evid-016'}) SET e.description='Informe del ente regulador de energía sobre medidores inteligentes',       e.url='https://ente.example/medidores-faq',          e.type='AGENCY_STATEMENT',       e.createdAt=datetime('2026-04-18T18:00:00');
MERGE (e:Evidence {id:'evid-017'}) SET e.description='Comunicado del banco central que descarta restricciones a los depósitos',  e.url='https://bcentral.example/depositos',          e.type='AGENCY_STATEMENT',       e.createdAt=datetime('2026-04-19T18:30:00');
MERGE (e:Evidence {id:'evid-018'}) SET e.description='Parte oficial de la policía que desmiente la cadena de secuestros',        e.url='https://policia.example/desmentida',          e.type='OFFICIAL_REPORT',        e.createdAt=datetime('2026-04-20T20:00:00');
MERGE (e:Evidence {id:'evid-019'}) SET e.description='Evaluación de la agencia de seguridad alimentaria sobre el edulcorante',   e.url='https://alimentos.example/edulcorante',       e.type='HEALTH_AUTHORITY',       e.createdAt=datetime('2026-04-21T17:00:00');
MERGE (e:Evidence {id:'evid-020'}) SET e.description='Revisión científica sobre efectos del wifi en la salud humana',           e.url='https://journal.example/wifi-salud',          e.type='SCIENTIFIC_PUBLICATION', e.createdAt=datetime('2026-04-22T20:00:00');
MERGE (e:Evidence {id:'evid-021'}) SET e.description='Comunicado de la autoridad de transporte sobre la nueva línea eléctrica',  e.url='https://transporte.example/linea-electrica',  e.type='AGENCY_STATEMENT',       e.createdAt=datetime('2026-04-05T08:00:00');
MERGE (e:Evidence {id:'evid-022'}) SET e.description='Relevamiento preliminar e incompleto de stock y precios en farmacias',     e.url='https://datos.example/farmacias-preliminar',  e.type='DATA_SERIES',            e.createdAt=datetime('2026-04-08T17:00:00');


// ============================================================
// 8. FACTCHECKS (12) — fc-009..fc-020
// ============================================================

MERGE (f:FactCheck {id:'fc-009'}) SET f.verdict='TRUE',           f.explanation='La resolución oficial confirma la incorporación de las dos vacunas al esquema.',          f.publishedAt=datetime('2026-04-02T10:00:00'), f.confidence=0.95;
MERGE (f:FactCheck {id:'fc-010'}) SET f.verdict='TRUE',           f.explanation='El paper indexado respalda la mejora de durabilidad reportada para la batería.',          f.publishedAt=datetime('2026-04-04T10:00:00'), f.confidence=0.93;
MERGE (f:FactCheck {id:'fc-011'}) SET f.verdict='PARTIALLY_TRUE', f.explanation='Hay demoras puntuales de reposición, pero no un desabastecimiento general confirmado.',   f.publishedAt=datetime('2026-04-09T11:00:00'), f.confidence=0.62;
MERGE (f:FactCheck {id:'fc-012'}) SET f.verdict='MISLEADING',     f.explanation='La proyección toma el escenario más extremo de un informe y lo presenta como certeza.',    f.publishedAt=datetime('2026-04-10T12:00:00'), f.confidence=0.74;
MERGE (f:FactCheck {id:'fc-013'}) SET f.verdict='PARTIALLY_TRUE', f.explanation='Un lote puntual tuvo observaciones; no se generaliza a toda la marca.',                  f.publishedAt=datetime('2026-04-13T12:00:00'), f.confidence=0.58;
MERGE (f:FactCheck {id:'fc-014'}) SET f.verdict='MISLEADING',     f.explanation='El 40% surge de un borrador no confirmado; no hay cifra oficial.',                        f.publishedAt=datetime('2026-04-15T12:00:00'), f.confidence=0.66;
MERGE (f:FactCheck {id:'fc-015'}) SET f.verdict='PARTIALLY_TRUE', f.explanation='La app orienta sobre síntomas, pero no diagnostica enfermedades como afirma el título.',  f.publishedAt=datetime('2026-04-16T12:00:00'), f.confidence=0.60;
MERGE (f:FactCheck {id:'fc-016'}) SET f.verdict='FALSE',          f.explanation='No existe evidencia clínica de que un té cure la diabetes; las sociedades médicas lo niegan.', f.publishedAt=datetime('2026-04-18T12:00:00'), f.confidence=0.97;
MERGE (f:FactCheck {id:'fc-017'}) SET f.verdict='FALSE',          f.explanation='Los medidores inteligentes no captan audio; el regulador lo descarta técnicamente.',      f.publishedAt=datetime('2026-04-19T12:00:00'), f.confidence=0.95;
MERGE (f:FactCheck {id:'fc-018'}) SET f.verdict='FALSE',          f.explanation='El banco central desmiente cualquier restricción a los depósitos.',                       f.publishedAt=datetime('2026-04-20T10:00:00'), f.confidence=0.96;
MERGE (f:FactCheck {id:'fc-019'}) SET f.verdict='FALSE',          f.explanation='La policía emitió un parte oficial desmintiendo la cadena de secuestros.',                f.publishedAt=datetime('2026-04-21T10:00:00'), f.confidence=0.97;
MERGE (f:FactCheck {id:'fc-020'}) SET f.verdict='FALSE',          f.explanation='La agencia de seguridad alimentaria aclara que el consumo moderado del edulcorante es seguro.', f.publishedAt=datetime('2026-04-22T10:00:00'), f.confidence=0.96;


// ============================================================
// 9. RELACIONES
// ============================================================

// --- News -[:PUBLISHED_BY {firstSeenAt, sourceUrl}]-> Source ---

MATCH (n:News {id:'news-009'}), (s:Source {id:'src-009'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-01T09:00:00'), r.sourceUrl='https://agenciacentral.example/salud/calendario-vacunacion';
MATCH (n:News {id:'news-010'}), (s:Source {id:'src-009'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-02T10:30:00'), r.sourceUrl='https://agenciacentral.example/educacion/alfabetizacion-digital';
MATCH (n:News {id:'news-011'}), (s:Source {id:'src-010'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-03T11:15:00'), r.sourceUrl='https://divulgaciencia.example/tecnologia/bateria-litio';
MATCH (n:News {id:'news-012'}), (s:Source {id:'src-011'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-04T16:00:00'), r.sourceUrl='https://observatorioeco.example/economia/tasa-interes';
MATCH (n:News {id:'news-013'}), (s:Source {id:'src-009'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-05T08:45:00'), r.sourceUrl='https://agenciacentral.example/transporte/colectivos-electricos';
MATCH (n:News {id:'news-014'}), (s:Source {id:'src-011'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-06T12:00:00'), r.sourceUrl='https://observatorioeco.example/energia/parque-solar';
MATCH (n:News {id:'news-015'}), (s:Source {id:'src-010'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-07T09:30:00'), r.sourceUrl='https://divulgaciencia.example/ambiente/reforestacion-urbana';
MATCH (n:News {id:'news-016'}), (s:Source {id:'src-012'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-08T18:20:00'), r.sourceUrl='https://portalmetropoli.example/salud/faltante-medicamento';
MATCH (n:News {id:'news-017'}), (s:Source {id:'src-014'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-09T20:10:00'), r.sourceUrl='https://tendenciasdiarias.example/economia/salto-dolar';
MATCH (n:News {id:'news-018'}), (s:Source {id:'src-013'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-10T19:40:00'), r.sourceUrl='https://lavozregional.example/politica/maniobra-sesion';
MATCH (n:News {id:'news-019'}), (s:Source {id:'src-013'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-11T21:00:00'), r.sourceUrl='https://lavozregional.example/seguridad/ola-robos';
MATCH (n:News {id:'news-020'}), (s:Source {id:'src-014'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-12T17:30:00'), r.sourceUrl='https://tendenciasdiarias.example/consumo/aceite-calidad';
MATCH (n:News {id:'news-021'}), (s:Source {id:'src-012'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-13T11:50:00'), r.sourceUrl='https://portalmetropoli.example/clima/verano-extremo';
MATCH (n:News {id:'news-022'}), (s:Source {id:'src-014'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-14T08:20:00'), r.sourceUrl='https://tendenciasdiarias.example/transporte/tarifas-suba';
MATCH (n:News {id:'news-023'}), (s:Source {id:'src-012'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-15T13:25:00'), r.sourceUrl='https://portalmetropoli.example/tecnologia/app-deteccion';
MATCH (n:News {id:'news-024'}), (s:Source {id:'src-013'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-16T10:05:00'), r.sourceUrl='https://lavozregional.example/educacion/regimen-examenes';
MATCH (n:News {id:'news-025'}), (s:Source {id:'src-016'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-17T22:15:00'), r.sourceUrl='https://misterioglobal.example/salud/te-cura-diabetes';
MATCH (n:News {id:'news-026'}), (s:Source {id:'src-016'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-18T23:00:00'), r.sourceUrl='https://misterioglobal.example/energia/medidores-espian';
MATCH (n:News {id:'news-027'}), (s:Source {id:'src-015'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-19T20:30:00'), r.sourceUrl='https://alertaviral24.example/economia/corralito-inminente';
MATCH (n:News {id:'news-028'}), (s:Source {id:'src-015'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-20T21:45:00'), r.sourceUrl='https://alertaviral24.example/seguridad/secuestros-app';
MATCH (n:News {id:'news-029'}), (s:Source {id:'src-016'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-21T19:10:00'), r.sourceUrl='https://misterioglobal.example/consumo/edulcorante-cancer';
MATCH (n:News {id:'news-030'}), (s:Source {id:'src-015'}) MERGE (n)-[r:PUBLISHED_BY]->(s) SET r.firstSeenAt=datetime('2026-04-22T22:40:00'), r.sourceUrl='https://alertaviral24.example/tecnologia/wifi-adn';


// --- News -[:ABOUT {relevance, source}]-> Topic ---

MATCH (n:News {id:'news-009'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95, r.source='manual_seed';
MATCH (n:News {id:'news-010'}), (t:Topic {id:'topic-008'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-011'}), (t:Topic {id:'topic-004'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.90, r.source='manual_seed';
MATCH (n:News {id:'news-011'}), (t:Topic {id:'topic-010'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.50, r.source='manual_seed';
MATCH (n:News {id:'news-012'}), (t:Topic {id:'topic-003'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-013'}), (t:Topic {id:'topic-009'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-014'}), (t:Topic {id:'topic-010'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95, r.source='manual_seed';
MATCH (n:News {id:'news-014'}), (t:Topic {id:'topic-012'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.60, r.source='manual_seed';
MATCH (n:News {id:'news-015'}), (t:Topic {id:'topic-012'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95, r.source='manual_seed';
MATCH (n:News {id:'news-016'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.90, r.source='manual_seed';
MATCH (n:News {id:'news-017'}), (t:Topic {id:'topic-003'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-018'}), (t:Topic {id:'topic-001'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-019'}), (t:Topic {id:'topic-007'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-020'}), (t:Topic {id:'topic-011'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95, r.source='manual_seed';
MATCH (n:News {id:'news-021'}), (t:Topic {id:'topic-005'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.80, r.source='manual_seed';
MATCH (n:News {id:'news-021'}), (t:Topic {id:'topic-012'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.60, r.source='manual_seed';
MATCH (n:News {id:'news-022'}), (t:Topic {id:'topic-009'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-023'}), (t:Topic {id:'topic-004'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.90, r.source='manual_seed';
MATCH (n:News {id:'news-023'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.50, r.source='manual_seed';
MATCH (n:News {id:'news-024'}), (t:Topic {id:'topic-008'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-025'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-026'}), (t:Topic {id:'topic-010'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.90, r.source='manual_seed';
MATCH (n:News {id:'news-026'}), (t:Topic {id:'topic-007'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.50, r.source='manual_seed';
MATCH (n:News {id:'news-027'}), (t:Topic {id:'topic-003'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-028'}), (t:Topic {id:'topic-007'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00, r.source='manual_seed';
MATCH (n:News {id:'news-029'}), (t:Topic {id:'topic-011'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.90, r.source='manual_seed';
MATCH (n:News {id:'news-029'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.60, r.source='manual_seed';
MATCH (n:News {id:'news-030'}), (t:Topic {id:'topic-004'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.80, r.source='manual_seed';
MATCH (n:News {id:'news-030'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.60, r.source='manual_seed';


// --- News -[:CONTAINS {extractedAt, extractionMethod}]-> Claim ---

MATCH (n:News {id:'news-009'}), (c:Claim {id:'claim-009'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-01T09:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-010'}), (c:Claim {id:'claim-010'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-02T11:00:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-011'}), (c:Claim {id:'claim-011'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-03T11:45:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-012'}), (c:Claim {id:'claim-012'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-04T16:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-013'}), (c:Claim {id:'claim-013'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-05T09:15:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-014'}), (c:Claim {id:'claim-014'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-06T12:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-015'}), (c:Claim {id:'claim-015'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-07T10:00:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-016'}), (c:Claim {id:'claim-016'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-08T18:45:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-017'}), (c:Claim {id:'claim-017'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-09T20:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-018'}), (c:Claim {id:'claim-018'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-10T20:00:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-019'}), (c:Claim {id:'claim-019'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-11T21:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-020'}), (c:Claim {id:'claim-020'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-12T18:00:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-021'}), (c:Claim {id:'claim-021'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-13T12:10:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-022'}), (c:Claim {id:'claim-022'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-14T08:50:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-023'}), (c:Claim {id:'claim-023'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-15T13:50:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-024'}), (c:Claim {id:'claim-024'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-16T10:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-025'}), (c:Claim {id:'claim-025'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-17T22:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-026'}), (c:Claim {id:'claim-026'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-18T23:20:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-027'}), (c:Claim {id:'claim-027'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-19T20:45:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-028'}), (c:Claim {id:'claim-028'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-20T22:00:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-029'}), (c:Claim {id:'claim-029'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-21T19:30:00'), r.extractionMethod='manual_seed';
MATCH (n:News {id:'news-030'}), (c:Claim {id:'claim-030'}) MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-04-22T23:00:00'), r.extractionMethod='manual_seed';


// --- User -[:CREATED {createdAt, deviceType}]-> Post ---

MATCH (u:User {id:'user-020'}), (p:Post {id:'post-020'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-01T09:30:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-022'}), (p:Post {id:'post-021'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-02T11:00:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-020'}), (p:Post {id:'post-022'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-03T11:45:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-021'}), (p:Post {id:'post-023'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-04T16:30:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-023'}), (p:Post {id:'post-024'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-05T09:15:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-022'}), (p:Post {id:'post-025'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-06T12:30:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-024'}), (p:Post {id:'post-026'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-07T10:00:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-025'}), (p:Post {id:'post-027'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-08T18:45:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-025'}), (p:Post {id:'post-028'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-09T20:30:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-026'}), (p:Post {id:'post-029'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-10T20:00:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-029'}), (p:Post {id:'post-030'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-11T21:30:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-026'}), (p:Post {id:'post-031'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-12T18:00:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-031'}), (p:Post {id:'post-032'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-13T12:10:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-030'}), (p:Post {id:'post-033'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-14T08:50:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-031'}), (p:Post {id:'post-034'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-15T13:50:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-032'}), (p:Post {id:'post-035'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-16T10:30:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-027'}), (p:Post {id:'post-036'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-17T22:30:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-027'}), (p:Post {id:'post-037'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-18T23:20:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-028'}), (p:Post {id:'post-038'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-19T20:45:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-028'}), (p:Post {id:'post-039'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-20T22:00:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-030'}), (p:Post {id:'post-040'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-21T19:30:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-027'}), (p:Post {id:'post-041'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-22T23:00:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-027'}), (p:Post {id:'post-042'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-19T21:15:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-027'}), (p:Post {id:'post-043'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-20T22:30:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-025'}), (p:Post {id:'post-044'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-04-18T08:00:00'), r.deviceType='MOBILE_IOS';


// --- Post -[:SPREADS {observedAt, reach, engagementCount}]-> News ---

MATCH (p:Post {id:'post-020'}), (n:News {id:'news-009'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-01T09:40:00'), r.reach=3200,  r.engagementCount=280;
MATCH (p:Post {id:'post-021'}), (n:News {id:'news-010'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-02T11:10:00'), r.reach=2100,  r.engagementCount=190;
MATCH (p:Post {id:'post-022'}), (n:News {id:'news-011'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-03T11:55:00'), r.reach=4100,  r.engagementCount=360;
MATCH (p:Post {id:'post-023'}), (n:News {id:'news-012'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-04T16:40:00'), r.reach=2600,  r.engagementCount=210;
MATCH (p:Post {id:'post-024'}), (n:News {id:'news-013'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-05T09:25:00'), r.reach=1800,  r.engagementCount=150;
MATCH (p:Post {id:'post-025'}), (n:News {id:'news-014'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-06T12:40:00'), r.reach=2200,  r.engagementCount=170;
MATCH (p:Post {id:'post-026'}), (n:News {id:'news-015'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-07T10:10:00'), r.reach=1500,  r.engagementCount=120;
MATCH (p:Post {id:'post-027'}), (n:News {id:'news-016'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-08T18:55:00'), r.reach=9800,  r.engagementCount=1400;
MATCH (p:Post {id:'post-028'}), (n:News {id:'news-017'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-09T20:40:00'), r.reach=14000, r.engagementCount=2600;
MATCH (p:Post {id:'post-029'}), (n:News {id:'news-018'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-10T20:10:00'), r.reach=8700,  r.engagementCount=1500;
MATCH (p:Post {id:'post-030'}), (n:News {id:'news-019'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-11T21:40:00'), r.reach=7200,  r.engagementCount=1100;
MATCH (p:Post {id:'post-031'}), (n:News {id:'news-020'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-12T18:10:00'), r.reach=10200, r.engagementCount=1700;
MATCH (p:Post {id:'post-032'}), (n:News {id:'news-021'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-13T12:20:00'), r.reach=8900,  r.engagementCount=1300;
MATCH (p:Post {id:'post-033'}), (n:News {id:'news-022'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-14T09:00:00'), r.reach=15600, r.engagementCount=2900;
MATCH (p:Post {id:'post-034'}), (n:News {id:'news-023'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-15T14:00:00'), r.reach=9300,  r.engagementCount=1600;
MATCH (p:Post {id:'post-035'}), (n:News {id:'news-024'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-16T10:40:00'), r.reach=6100,  r.engagementCount=900;
MATCH (p:Post {id:'post-036'}), (n:News {id:'news-025'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-17T22:40:00'), r.reach=28000, r.engagementCount=5200;
MATCH (p:Post {id:'post-037'}), (n:News {id:'news-026'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-18T23:30:00'), r.reach=21000, r.engagementCount=4100;
MATCH (p:Post {id:'post-038'}), (n:News {id:'news-027'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-19T20:55:00'), r.reach=33000, r.engagementCount=7400;
MATCH (p:Post {id:'post-039'}), (n:News {id:'news-028'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-20T22:10:00'), r.reach=41000, r.engagementCount=9100;
MATCH (p:Post {id:'post-040'}), (n:News {id:'news-029'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-21T19:40:00'), r.reach=18000, r.engagementCount=3300;
MATCH (p:Post {id:'post-041'}), (n:News {id:'news-030'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-22T23:10:00'), r.reach=26000, r.engagementCount=4800;
MATCH (p:Post {id:'post-042'}), (n:News {id:'news-027'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-19T21:25:00'), r.reach=12000, r.engagementCount=2100;
MATCH (p:Post {id:'post-043'}), (n:News {id:'news-028'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-20T22:40:00'), r.reach=15000, r.engagementCount=2700;
MATCH (p:Post {id:'post-044'}), (n:News {id:'news-025'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-04-18T08:10:00'), r.reach=9000,  r.engagementCount=1500;


// --- User -[:SHARED {sharedAt, shareType, reach}]-> Post --- (difusión coordinada en HIGH)

MATCH (u:User {id:'user-027'}), (p:Post {id:'post-039'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-20T22:20:00'), r.shareType='QUOTE',    r.reach=30000;
MATCH (u:User {id:'user-028'}), (p:Post {id:'post-038'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-19T21:00:00'), r.shareType='REPOST',   r.reach=12000;
MATCH (u:User {id:'user-025'}), (p:Post {id:'post-038'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-19T21:30:00'), r.shareType='REPOST',   r.reach=50000;
MATCH (u:User {id:'user-030'}), (p:Post {id:'post-041'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-22T23:30:00'), r.shareType='REPOST',   r.reach=4000;
MATCH (u:User {id:'user-024'}), (p:Post {id:'post-036'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-17T23:00:00'), r.shareType='REACTION', r.reach=600;
MATCH (u:User {id:'user-026'}), (p:Post {id:'post-031'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-04-12T18:30:00'), r.shareType='REPOST',   r.reach=300;


// --- User -[:FOLLOWS {since, interactionStrength}]-> User ---

MATCH (a:User {id:'user-028'}), (b:User {id:'user-027'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2026-01-25'), r.interactionStrength=0.90;
MATCH (a:User {id:'user-027'}), (b:User {id:'user-028'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2026-01-25'), r.interactionStrength=0.90;
MATCH (a:User {id:'user-025'}), (b:User {id:'user-027'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2026-02-10'), r.interactionStrength=0.70;
MATCH (a:User {id:'user-030'}), (b:User {id:'user-025'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-12-01'), r.interactionStrength=0.60;
MATCH (a:User {id:'user-024'}), (b:User {id:'user-020'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-06-01'), r.interactionStrength=0.50;
MATCH (a:User {id:'user-026'}), (b:User {id:'user-022'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-08-15'), r.interactionStrength=0.55;
MATCH (a:User {id:'user-023'}), (b:User {id:'user-021'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-05-20'), r.interactionStrength=0.60;


// --- User -[:INTERACTS_WITH {interactionType, weight, lastInteractionAt}]-> User ---

MATCH (a:User {id:'user-027'}), (b:User {id:'user-028'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='QUOTE',   r.weight=0.80, r.lastInteractionAt=datetime('2026-04-20T22:30:00');
MATCH (a:User {id:'user-025'}), (b:User {id:'user-027'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='MENTION', r.weight=0.50, r.lastInteractionAt=datetime('2026-04-19T21:30:00');
MATCH (a:User {id:'user-030'}), (b:User {id:'user-031'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='REPLY',   r.weight=0.40, r.lastInteractionAt=datetime('2026-04-14T09:00:00');
MATCH (a:User {id:'user-021'}), (b:User {id:'user-020'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='REPLY',   r.weight=0.60, r.lastInteractionAt=datetime('2026-04-04T17:00:00');


// --- Claim -[:SUPPORTED_BY {confidence, note}]-> Evidence ---

MATCH (c:Claim {id:'claim-009'}), (e:Evidence {id:'evid-009'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.95, r.note='Resolución oficial confirma la ampliación del calendario.';
MATCH (c:Claim {id:'claim-010'}), (e:Evidence {id:'evid-010'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.90, r.note='Informe de cobertura respalda el alcance de 1.200 escuelas.';
MATCH (c:Claim {id:'claim-011'}), (e:Evidence {id:'evid-011'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.93, r.note='Paper indexado documenta la mejora de durabilidad.';
MATCH (c:Claim {id:'claim-012'}), (e:Evidence {id:'evid-012'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.95, r.note='Comunicado del banco central confirma la tasa sin cambios.';
MATCH (c:Claim {id:'claim-013'}), (e:Evidence {id:'evid-021'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.90, r.note='Comunicado de transporte confirma el corredor central.';
MATCH (c:Claim {id:'claim-014'}), (e:Evidence {id:'evid-013'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.92, r.note='Datos de generación respaldan el aporte energético.';
MATCH (c:Claim {id:'claim-015'}), (e:Evidence {id:'evid-014'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.90, r.note='Registro municipal confirma la cantidad de árboles plantados.';
MATCH (c:Claim {id:'claim-016'}), (e:Evidence {id:'evid-022'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.45, r.note='Relevamiento preliminar e incompleto: sugiere demoras puntuales, no desabastecimiento.';
MATCH (c:Claim {id:'claim-020'}), (e:Evidence {id:'evid-022'}) MERGE (c)-[r:SUPPORTED_BY]->(e) SET r.confidence=0.40, r.note='Evidencia incompleta; no permite generalizar a toda la marca.';


// --- Claim -[:REFUTED_BY {confidence, note}]-> Evidence ---

MATCH (c:Claim {id:'claim-025'}), (e:Evidence {id:'evid-015'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.97, r.note='La sociedad de endocrinología descarta curas caseras de la diabetes.';
MATCH (c:Claim {id:'claim-026'}), (e:Evidence {id:'evid-016'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.95, r.note='El ente regulador aclara que los medidores no captan audio.';
MATCH (c:Claim {id:'claim-027'}), (e:Evidence {id:'evid-017'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.96, r.note='El banco central desmiente restricciones a los depósitos.';
MATCH (c:Claim {id:'claim-028'}), (e:Evidence {id:'evid-018'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.97, r.note='Parte policial oficial desmiente la cadena de secuestros.';
MATCH (c:Claim {id:'claim-029'}), (e:Evidence {id:'evid-019'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.96, r.note='La agencia alimentaria aclara que el consumo moderado es seguro.';
MATCH (c:Claim {id:'claim-030'}), (e:Evidence {id:'evid-020'}) MERGE (c)-[r:REFUTED_BY]->(e) SET r.confidence=0.95, r.note='La revisión científica desmiente el daño al ADN por wifi.';


// --- FactCheck -[:CHECKS {checkedAt, method}]-> Claim ---

MATCH (f:FactCheck {id:'fc-009'}), (c:Claim {id:'claim-009'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-02T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-010'}), (c:Claim {id:'claim-011'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-04T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-011'}), (c:Claim {id:'claim-016'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-09T11:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-012'}), (c:Claim {id:'claim-017'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-10T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-013'}), (c:Claim {id:'claim-020'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-13T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-014'}), (c:Claim {id:'claim-022'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-15T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-015'}), (c:Claim {id:'claim-023'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-16T12:00:00'), r.method='AUTOMATED';
MATCH (f:FactCheck {id:'fc-016'}), (c:Claim {id:'claim-025'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-18T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-017'}), (c:Claim {id:'claim-026'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-19T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-018'}), (c:Claim {id:'claim-027'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-20T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-019'}), (c:Claim {id:'claim-028'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-21T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-020'}), (c:Claim {id:'claim-029'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-04-22T10:00:00'), r.method='MANUAL_REVIEW';


// --- FactCheck -[:BASED_ON {relevance, usedAs}]-> Evidence ---

MATCH (f:FactCheck {id:'fc-009'}), (e:Evidence {id:'evid-009'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.95, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-010'}), (e:Evidence {id:'evid-011'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.93, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-011'}), (e:Evidence {id:'evid-022'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.60, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-012'}), (e:Evidence {id:'evid-012'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.70, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-013'}), (e:Evidence {id:'evid-022'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.55, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-014'}), (e:Evidence {id:'evid-021'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.65, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-015'}), (e:Evidence {id:'evid-020'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.60, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-016'}), (e:Evidence {id:'evid-015'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.97, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-017'}), (e:Evidence {id:'evid-016'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.95, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-018'}), (e:Evidence {id:'evid-017'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.96, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-019'}), (e:Evidence {id:'evid-018'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.97, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-020'}), (e:Evidence {id:'evid-019'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.96, r.usedAs='PRIMARY';


// ============================================================
// 10. ASOCIAR LAS NOTICIAS NUEVAS A LA CUENTA DEMO
//     (:AppUser {email:'demo@nexoveraz.local'})-[:OWNS_NEWS]->(:News)
//
// Si la cuenta demo no existe, este bloque no vincula nada (MATCH vacío).
// En ese caso, levantar el backend una vez (DemoDataInitializer crea demo
// y enlaza las noticias sin dueño) o re-ejecutar este script luego.
// ============================================================

MATCH (demo:AppUser {email: 'demo@nexoveraz.local'})
UNWIND ['news-009','news-010','news-011','news-012','news-013','news-014','news-015',
        'news-016','news-017','news-018','news-019','news-020','news-021','news-022',
        'news-023','news-024','news-025','news-026','news-027','news-028','news-029','news-030'] AS nid
MATCH (n:News {id: nid})
MERGE (demo)-[r:OWNS_NEWS]->(n)
  ON CREATE SET r.origin = 'ADDITIONAL_SEED', r.createdAt = datetime();


// ============================================================
// FIN — ver cypher/05_verification_queries.cypher para validar.
// ============================================================
