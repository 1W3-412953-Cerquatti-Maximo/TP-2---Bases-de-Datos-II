// ============================================================
// 02_seed_data.cypher
// Datos sembrados realistas — NexoVeraz (App Anti Fake News)
// TP-2 Bases de Datos II
//
// Cubre los 8 tipos de nodos del modelo y carga propiedades
// tanto en nodos como en relaciones (peso, fechas, contexto).
//
// Convenciones:
//   * riskScore ∈ [0, 100]   — propiedad de :News
//   * riskLevel ∈ {LOW, MEDIUM, HIGH}
//       LOW    → 0..39
//       MEDIUM → 40..69
//       HIGH   → 70..100
//   * credibilityScore ∈ [0, 1] — propiedad de :Source
//   * confidence/relevance/weight/interactionStrength ∈ [0, 1]
//
// Ejecutar DESPUÉS de 01_constraints.cypher.
// Idempotente: re-ejecutar no duplica nodos (MERGE) y refresca
// las propiedades de relaciones (SET sobreescribe).
//
// Todos los datos son FICTICIOS.
// ============================================================


// ============================================================
// 1. SOURCES (6) — medios de distinta credibilidad
// ============================================================

MERGE (s1:Source {id: 'src-001'})
SET s1.name = 'Diario Veracidad',     s1.url = 'https://veracidad.example',
    s1.credibilityScore = 0.88,       s1.type = 'newspaper';

MERGE (s2:Source {id: 'src-002'})
SET s2.name = 'CientíficaHoy',        s2.url = 'https://cientificahoy.example',
    s2.credibilityScore = 0.92,       s2.type = 'science_outlet';

MERGE (s3:Source {id: 'src-003'})
SET s3.name = 'PortalAhora',          s3.url = 'https://portalahora.example',
    s3.credibilityScore = 0.65,       s3.type = 'digital_news';

MERGE (s4:Source {id: 'src-004'})
SET s4.name = 'TribunaPopular',       s4.url = 'https://tribunapopular.example',
    s4.credibilityScore = 0.45,       s4.type = 'tabloid';

MERGE (s5:Source {id: 'src-005'})
SET s5.name = 'VerdadOculta',         s5.url = 'https://verdadoculta.example',
    s5.credibilityScore = 0.18,       s5.type = 'conspiracy_blog';

MERGE (s6:Source {id: 'src-006'})
SET s6.name = 'Click24',              s6.url = 'https://click24.example',
    s6.credibilityScore = 0.28,       s6.type = 'clickbait';


// ============================================================
// 2. TOPICS (6)
// ============================================================

MERGE (t1:Topic {id: 'topic-001'}) SET t1.name = 'Política',             t1.slug = 'politica';
MERGE (t2:Topic {id: 'topic-002'}) SET t2.name = 'Salud',                t2.slug = 'salud';
MERGE (t3:Topic {id: 'topic-003'}) SET t3.name = 'Economía',             t3.slug = 'economia';
MERGE (t4:Topic {id: 'topic-004'}) SET t4.name = 'Ciencia y Tecnología', t4.slug = 'ciencia-tec';
MERGE (t5:Topic {id: 'topic-005'}) SET t5.name = 'Cambio Climático',     t5.slug = 'clima';
MERGE (t6:Topic {id: 'topic-006'}) SET t6.name = 'Deportes',             t6.slug = 'deportes';


// ============================================================
// 3. NEWS (8) — riskScore en escala 0..100, con riskLevel categorizado
// ============================================================

MERGE (n1:News {id: 'news-001'})
SET n1.title       = 'Gobierno presenta plan económico de tres etapas',
    n1.content     = 'El ministerio de Economía anunció medidas fiscales graduales para los próximos 18 meses, basadas en consenso con el sector privado.',
    n1.publishedAt = datetime('2026-03-10T09:30:00'),
    n1.url         = 'https://veracidad.example/economia/plan-tres-etapas',
    n1.riskScore   = 15, n1.riskLevel = 'LOW';

MERGE (n2:News {id: 'news-002'})
SET n2.title       = 'Investigadores reportan vacuna contra dengue con 87% de eficacia',
    n2.content     = 'Un consorcio académico publicó resultados de fase III en una revista indexada, con seguimiento a 12 meses.',
    n2.publishedAt = datetime('2026-03-12T14:00:00'),
    n2.url         = 'https://cientificahoy.example/salud/vacuna-dengue-fase3',
    n2.riskScore   = 10, n2.riskLevel = 'LOW';

MERGE (n3:News {id: 'news-003'})
SET n3.title       = 'El 5G estaría provocando una epidemia de dolores de cabeza',
    n3.content     = 'Fuentes anónimas aseguran que las nuevas antenas afectan la salud cerebral, aunque no se aportan estudios revisados por pares.',
    n3.publishedAt = datetime('2026-03-14T18:45:00'),
    n3.url         = 'https://verdadoculta.example/5g-epidemia',
    n3.riskScore   = 92, n3.riskLevel = 'HIGH';

MERGE (n4:News {id: 'news-004'})
SET n4.title       = 'Confirman presencia de agua líquida bajo la superficie de Marte',
    n4.content     = 'Un equipo internacional analizó datos de radar de una sonda y reportó indicios consistentes con agua salada subterránea.',
    n4.publishedAt = datetime('2026-03-15T11:00:00'),
    n4.url         = 'https://cientificahoy.example/ciencia/agua-marte',
    n4.riskScore   = 8, n4.riskLevel = 'LOW';

MERGE (n5:News {id: 'news-005'})
SET n5.title       = 'Antártida supera récord histórico de temperatura en marzo',
    n5.content     = 'Estaciones meteorológicas registraron valores anómalos, en línea con la tendencia regional documentada.',
    n5.publishedAt = datetime('2026-03-17T08:15:00'),
    n5.url         = 'https://portalahora.example/clima/antartida-record',
    n5.riskScore   = 20, n5.riskLevel = 'LOW';

MERGE (n6:News {id: 'news-006'})
SET n6.title       = 'Crisis económica inminente: economistas hablan de "colapso en 6 meses"',
    n6.content     = 'El titular toma una frase aislada de una entrevista y la presenta como predicción general del sector.',
    n6.publishedAt = datetime('2026-03-18T20:00:00'),
    n6.url         = 'https://click24.example/economia/colapso-6-meses',
    n6.riskScore   = 78, n6.riskLevel = 'HIGH';

MERGE (n7:News {id: 'news-007'})
SET n7.title       = 'Las estelas de los aviones son armas químicas, dice "experto"',
    n7.content     = 'Un autor sin afiliación académica retoma la teoría de los chemtrails, ya desmentida múltiples veces por agencias atmosféricas.',
    n7.publishedAt = datetime('2026-03-19T22:30:00'),
    n7.url         = 'https://verdadoculta.example/chemtrails-confirmados',
    n7.riskScore   = 95, n7.riskLevel = 'HIGH';

MERGE (n8:News {id: 'news-008'})
SET n8.title       = 'Selección anuncia convocatoria para la próxima fecha FIFA',
    n8.content     = 'El cuerpo técnico publicó la lista oficial de 26 jugadores convocados para los amistosos de abril.',
    n8.publishedAt = datetime('2026-03-20T10:00:00'),
    n8.url         = 'https://veracidad.example/deportes/convocatoria-abril',
    n8.riskScore   = 5, n8.riskLevel = 'LOW';


// ============================================================
// 4. USERS (10)
// ============================================================

MERGE (u1:User  {id: 'user-001'}) SET u1.username='fact_checker_ana',  u1.role='FACT_CHECKER', u1.followerCount=5400,  u1.suspicious=false, u1.createdAt=datetime('2025-01-10T00:00:00');
MERGE (u2:User  {id: 'user-002'}) SET u2.username='journalist_lucas',  u2.role='JOURNALIST',   u2.followerCount=12800, u2.suspicious=false, u2.createdAt=datetime('2025-02-01T00:00:00');
MERGE (u3:User  {id: 'user-003'}) SET u3.username='reader_carla',      u3.role='READER',       u3.followerCount=120,   u3.suspicious=false, u3.createdAt=datetime('2025-03-15T00:00:00');
MERGE (u4:User  {id: 'user-004'}) SET u4.username='reader_diego',      u4.role='READER',       u4.followerCount=85,    u4.suspicious=false, u4.createdAt=datetime('2025-04-22T00:00:00');
MERGE (u5:User  {id: 'user-005'}) SET u5.username='influencer_mateo',  u5.role='INFLUENCER',   u5.followerCount=98000, u5.suspicious=false, u5.createdAt=datetime('2025-05-05T00:00:00');
MERGE (u6:User  {id: 'user-006'}) SET u6.username='skeptic_lucia',     u6.role='READER',       u6.followerCount=3200,  u6.suspicious=true,  u6.createdAt=datetime('2025-06-11T00:00:00');
MERGE (u7:User  {id: 'user-007'}) SET u7.username='editor_pablo',      u7.role='EDITOR',       u7.followerCount=4100,  u7.suspicious=false, u7.createdAt=datetime('2025-07-19T00:00:00');
MERGE (u8:User  {id: 'user-008'}) SET u8.username='reader_sofia',      u8.role='READER',       u8.followerCount=540,   u8.suspicious=false, u8.createdAt=datetime('2025-08-02T00:00:00');
MERGE (u9:User  {id: 'user-009'}) SET u9.username='bot_account_001',   u9.role='SUSPICIOUS',   u9.followerCount=42,    u9.suspicious=true,  u9.createdAt=datetime('2026-01-04T00:00:00');
MERGE (u10:User {id: 'user-010'}) SET u10.username='reader_julian',    u10.role='READER',      u10.followerCount=290,  u10.suspicious=false, u10.createdAt=datetime('2025-09-30T00:00:00');


// ============================================================
// 5. POSTS (13)
// ============================================================

MERGE (p1:Post  {id: 'post-001'}) SET p1.content='Esto explica todo lo que está pasando con la economía 👇', p1.platform='TWITTER',   p1.createdAt=datetime('2026-03-10T10:00:00');
MERGE (p2:Post  {id: 'post-002'}) SET p2.content='Excelente noticia para la salud pública.',                p2.platform='FACEBOOK',  p2.createdAt=datetime('2026-03-12T15:00:00');
MERGE (p3:Post  {id: 'post-003'}) SET p3.content='¡Atención! Esto es muy grave, compartan urgente.',         p3.platform='TELEGRAM',  p3.createdAt=datetime('2026-03-14T19:00:00');
MERGE (p4:Post  {id: 'post-004'}) SET p4.content='Mi opinión sobre el tema 5G — hilo 🧵',                    p4.platform='TWITTER',   p4.createdAt=datetime('2026-03-14T20:30:00');
MERGE (p5:Post  {id: 'post-005'}) SET p5.content='Increíble descubrimiento en Marte.',                       p5.platform='INSTAGRAM', p5.createdAt=datetime('2026-03-15T13:00:00');
MERGE (p6:Post  {id: 'post-006'}) SET p6.content='La crisis ya está acá, prepárense.',                       p6.platform='FACEBOOK',  p6.createdAt=datetime('2026-03-18T21:00:00');
MERGE (p7:Post  {id: 'post-007'}) SET p7.content='Lo que el sistema no quiere que veas.',                    p7.platform='TELEGRAM',  p7.createdAt=datetime('2026-03-19T23:00:00');
MERGE (p8:Post  {id: 'post-008'}) SET p8.content='Convocatoria selección abril — el 9 va!',                   p8.platform='TWITTER',   p8.createdAt=datetime('2026-03-20T10:30:00');
MERGE (p9:Post  {id: 'post-009'}) SET p9.content='Datos preocupantes sobre el clima en la Antártida.',       p9.platform='TWITTER',   p9.createdAt=datetime('2026-03-17T09:00:00');
MERGE (p10:Post {id: 'post-010'}) SET p10.content='Compartiendo este artículo, léanlo entero.',              p10.platform='FACEBOOK', p10.createdAt=datetime('2026-03-14T21:00:00');
MERGE (p11:Post {id: 'post-011'}) SET p11.content='La verdad sobre las antenas y nuestra salud.',            p11.platform='TIKTOK',   p11.createdAt=datetime('2026-03-15T09:15:00');
MERGE (p12:Post {id: 'post-012'}) SET p12.content='Otro caso de manipulación mediática.',                    p12.platform='TWITTER',  p12.createdAt=datetime('2026-03-19T23:45:00');
MERGE (p13:Post {id: 'post-013'}) SET p13.content='Análisis serio del plan económico.',                       p13.platform='TWITTER',  p13.createdAt=datetime('2026-03-10T12:00:00');


// ============================================================
// 6. CLAIMS (8)
// ============================================================

MERGE (c1:Claim {id: 'claim-001'}) SET c1.text='El plan económico reducirá la inflación 30% en 6 meses.',         c1.status='UNDER_REVIEW', c1.createdAt=datetime('2026-03-11T09:00:00');
MERGE (c2:Claim {id: 'claim-002'}) SET c2.text='La vacuna contra el dengue alcanzó 87% de eficacia en fase III.', c2.status='VERIFIED',     c2.createdAt=datetime('2026-03-12T15:00:00');
MERGE (c3:Claim {id: 'claim-003'}) SET c3.text='Las antenas 5G provocan dolores de cabeza masivos.',              c3.status='REFUTED',      c3.createdAt=datetime('2026-03-14T19:00:00');
MERGE (c4:Claim {id: 'claim-004'}) SET c4.text='Hay agua líquida bajo el polo sur de Marte.',                     c4.status='VERIFIED',     c4.createdAt=datetime('2026-03-15T11:30:00');
MERGE (c5:Claim {id: 'claim-005'}) SET c5.text='La Antártida superó 20°C en marzo.',                              c5.status='VERIFIED',     c5.createdAt=datetime('2026-03-17T09:00:00');
MERGE (c6:Claim {id: 'claim-006'}) SET c6.text='Habrá un colapso económico total en 6 meses.',                    c6.status='REFUTED',      c6.createdAt=datetime('2026-03-18T20:30:00');
MERGE (c7:Claim {id: 'claim-007'}) SET c7.text='Las estelas de los aviones son armas químicas.',                   c7.status='REFUTED',      c7.createdAt=datetime('2026-03-19T22:45:00');
MERGE (c8:Claim {id: 'claim-008'}) SET c8.text='El jugador #9 fue convocado a la selección.',                      c8.status='VERIFIED',     c8.createdAt=datetime('2026-03-20T10:15:00');


// ============================================================
// 7. EVIDENCE (6)
// ============================================================

MERGE (e1:Evidence {id: 'evid-001'}) SET e1.description='Reporte oficial del organismo de estadísticas sobre inflación marzo 2026', e1.url='https://stats.example/ipc/03-2026',     e1.type='OFFICIAL_REPORT',        e1.createdAt=datetime('2026-03-16T10:00:00');
MERGE (e2:Evidence {id: 'evid-002'}) SET e2.description='Paper indexado sobre fase III de vacuna candidata contra dengue',          e2.url='https://journal.example/dengue-phase3', e2.type='SCIENTIFIC_PUBLICATION', e2.createdAt=datetime('2026-03-12T15:30:00');
MERGE (e3:Evidence {id: 'evid-003'}) SET e3.description='Posición de la OMS sobre efectos del 5G en la salud humana',               e3.url='https://who.example/5g-health',         e3.type='HEALTH_AUTHORITY',       e3.createdAt=datetime('2026-03-15T08:00:00');
MERGE (e4:Evidence {id: 'evid-004'}) SET e4.description='Comunicado de una agencia espacial sobre datos de radar de Marte',         e4.url='https://space.example/mars-water',      e4.type='AGENCY_STATEMENT',       e4.createdAt=datetime('2026-03-15T11:00:00');
MERGE (e5:Evidence {id: 'evid-005'}) SET e5.description='Serie histórica de temperaturas de la Antártida',                          e5.url='https://climate.example/antarctica',    e5.type='DATA_SERIES',            e5.createdAt=datetime('2026-03-17T09:30:00');
MERGE (e6:Evidence {id: 'evid-006'}) SET e6.description='Estudio atmosférico que desmiente la hipótesis de chemtrails',             e6.url='https://atmosci.example/contrails',     e6.type='SCIENTIFIC_PUBLICATION', e6.createdAt=datetime('2026-03-19T20:00:00');


// ============================================================
// 8. FACTCHECKS (5)
// ============================================================

MERGE (fc1:FactCheck {id: 'fc-001'}) SET fc1.verdict='PARTIALLY_TRUE', fc1.explanation='El plan apunta a reducir inflación pero la cifra de 30% en 6 meses no está respaldada.', fc1.publishedAt=datetime('2026-03-16T12:00:00'), fc1.confidence=0.83;
MERGE (fc2:FactCheck {id: 'fc-002'}) SET fc2.verdict='TRUE',           fc2.explanation='La cifra de 87% surge del paper indexado y coincide con el comunicado del consorcio.',    fc2.publishedAt=datetime('2026-03-13T10:00:00'), fc2.confidence=0.95;
MERGE (fc3:FactCheck {id: 'fc-003'}) SET fc3.verdict='FALSE',          fc3.explanation='No existe evidencia revisada por pares que vincule 5G con dolores de cabeza masivos.',   fc3.publishedAt=datetime('2026-03-15T16:00:00'), fc3.confidence=0.97;
MERGE (fc4:FactCheck {id: 'fc-004'}) SET fc4.verdict='MISLEADING',     fc4.explanation='La frase fue sacada de contexto: el economista hablaba de un escenario extremo.',         fc4.publishedAt=datetime('2026-03-19T10:00:00'), fc4.confidence=0.88;
MERGE (fc5:FactCheck {id: 'fc-005'}) SET fc5.verdict='FALSE',          fc5.explanation='La hipótesis chemtrails carece de fundamento físico-químico y ha sido desmentida.',     fc5.publishedAt=datetime('2026-03-20T11:00:00'), fc5.confidence=0.98;


// ============================================================
// 9. RELACIONES (con propiedades)
// ============================================================

// --- News -[:PUBLISHED_BY {firstSeenAt, sourceUrl}]-> Source ---

MATCH (n:News {id:'news-001'}), (s:Source {id:'src-001'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-10T09:30:00'),
    r.sourceUrl   = 'https://veracidad.example/economia/plan-tres-etapas';

MATCH (n:News {id:'news-002'}), (s:Source {id:'src-002'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-12T14:00:00'),
    r.sourceUrl   = 'https://cientificahoy.example/salud/vacuna-dengue-fase3';

MATCH (n:News {id:'news-003'}), (s:Source {id:'src-005'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-14T18:45:00'),
    r.sourceUrl   = 'https://verdadoculta.example/5g-epidemia';

MATCH (n:News {id:'news-004'}), (s:Source {id:'src-002'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-15T11:00:00'),
    r.sourceUrl   = 'https://cientificahoy.example/ciencia/agua-marte';

MATCH (n:News {id:'news-005'}), (s:Source {id:'src-003'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-17T08:15:00'),
    r.sourceUrl   = 'https://portalahora.example/clima/antartida-record';

MATCH (n:News {id:'news-006'}), (s:Source {id:'src-006'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-18T20:00:00'),
    r.sourceUrl   = 'https://click24.example/economia/colapso-6-meses';

MATCH (n:News {id:'news-007'}), (s:Source {id:'src-005'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-19T22:30:00'),
    r.sourceUrl   = 'https://verdadoculta.example/chemtrails-confirmados';

MATCH (n:News {id:'news-008'}), (s:Source {id:'src-001'})
MERGE (n)-[r:PUBLISHED_BY]->(s)
SET r.firstSeenAt = datetime('2026-03-20T10:00:00'),
    r.sourceUrl   = 'https://veracidad.example/deportes/convocatoria-abril';


// --- News -[:CONTAINS {extractedAt, extractionMethod}]-> Claim ---

MATCH (n:News {id:'news-001'}), (c:Claim {id:'claim-001'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-11T09:00:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-002'}), (c:Claim {id:'claim-002'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-12T15:00:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-003'}), (c:Claim {id:'claim-003'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-14T19:00:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-004'}), (c:Claim {id:'claim-004'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-15T11:30:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-005'}), (c:Claim {id:'claim-005'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-17T09:00:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-006'}), (c:Claim {id:'claim-006'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-18T20:30:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-007'}), (c:Claim {id:'claim-007'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-19T22:45:00'), r.extractionMethod='manual_seed';

MATCH (n:News {id:'news-008'}), (c:Claim {id:'claim-008'})
MERGE (n)-[r:CONTAINS]->(c) SET r.extractedAt=datetime('2026-03-20T10:15:00'), r.extractionMethod='manual_seed';


// --- News -[:ABOUT {relevance}]-> Topic ---

MATCH (n:News {id:'news-001'}), (t:Topic {id:'topic-001'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.70;
MATCH (n:News {id:'news-001'}), (t:Topic {id:'topic-003'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95;
MATCH (n:News {id:'news-002'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00;
MATCH (n:News {id:'news-003'}), (t:Topic {id:'topic-002'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.60;
MATCH (n:News {id:'news-003'}), (t:Topic {id:'topic-004'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.85;
MATCH (n:News {id:'news-004'}), (t:Topic {id:'topic-004'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00;
MATCH (n:News {id:'news-005'}), (t:Topic {id:'topic-005'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00;
MATCH (n:News {id:'news-006'}), (t:Topic {id:'topic-003'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.95;
MATCH (n:News {id:'news-007'}), (t:Topic {id:'topic-005'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=0.70;
MATCH (n:News {id:'news-008'}), (t:Topic {id:'topic-006'}) MERGE (n)-[r:ABOUT]->(t) SET r.relevance=1.00;


// --- User -[:CREATED {createdAt, deviceType}]-> Post ---

MATCH (u:User {id:'user-002'}), (p:Post {id:'post-001'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-10T10:00:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-001'}), (p:Post {id:'post-002'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-12T15:00:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-006'}), (p:Post {id:'post-003'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-14T19:00:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-005'}), (p:Post {id:'post-004'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-14T20:30:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-007'}), (p:Post {id:'post-005'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-15T13:00:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-009'}), (p:Post {id:'post-006'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-18T21:00:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-009'}), (p:Post {id:'post-007'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-19T23:00:00'), r.deviceType='BOT_API';
MATCH (u:User {id:'user-002'}), (p:Post {id:'post-008'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-20T10:30:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-007'}), (p:Post {id:'post-009'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-17T09:00:00'), r.deviceType='WEB';
MATCH (u:User {id:'user-005'}), (p:Post {id:'post-010'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-14T21:00:00'), r.deviceType='MOBILE_IOS';
MATCH (u:User {id:'user-006'}), (p:Post {id:'post-011'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-15T09:15:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-006'}), (p:Post {id:'post-012'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-19T23:45:00'), r.deviceType='MOBILE_ANDROID';
MATCH (u:User {id:'user-002'}), (p:Post {id:'post-013'}) MERGE (u)-[r:CREATED]->(p) SET r.createdAt=datetime('2026-03-10T12:00:00'), r.deviceType='WEB';


// --- Post -[:SPREADS {observedAt, reach, engagementCount}]-> News ---

MATCH (p:Post {id:'post-001'}), (n:News {id:'news-001'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-10T10:05:00'), r.reach=8500,  r.engagementCount=230;
MATCH (p:Post {id:'post-002'}), (n:News {id:'news-002'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-12T15:10:00'), r.reach=3200,  r.engagementCount=540;
MATCH (p:Post {id:'post-003'}), (n:News {id:'news-003'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-14T19:10:00'), r.reach=24000, r.engagementCount=4200;
MATCH (p:Post {id:'post-004'}), (n:News {id:'news-003'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-14T20:40:00'), r.reach=75000, r.engagementCount=9800;
MATCH (p:Post {id:'post-005'}), (n:News {id:'news-004'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-15T13:10:00'), r.reach=2100,  r.engagementCount=410;
MATCH (p:Post {id:'post-006'}), (n:News {id:'news-006'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-18T21:10:00'), r.reach=5500,  r.engagementCount=980;
MATCH (p:Post {id:'post-007'}), (n:News {id:'news-007'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-19T23:10:00'), r.reach=18000, r.engagementCount=3100;
MATCH (p:Post {id:'post-008'}), (n:News {id:'news-008'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-20T10:40:00'), r.reach=6900,  r.engagementCount=1100;
MATCH (p:Post {id:'post-009'}), (n:News {id:'news-005'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-17T09:10:00'), r.reach=1800,  r.engagementCount=320;
MATCH (p:Post {id:'post-010'}), (n:News {id:'news-003'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-14T21:10:00'), r.reach=12000, r.engagementCount=2400;
MATCH (p:Post {id:'post-011'}), (n:News {id:'news-003'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-15T09:25:00'), r.reach=31000, r.engagementCount=7800;
MATCH (p:Post {id:'post-012'}), (n:News {id:'news-007'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-19T23:55:00'), r.reach=4800,  r.engagementCount=1100;
MATCH (p:Post {id:'post-013'}), (n:News {id:'news-001'}) MERGE (p)-[r:SPREADS]->(n) SET r.observedAt=datetime('2026-03-10T12:10:00'), r.reach=2400,  r.engagementCount=380;


// --- User -[:SHARED {sharedAt, shareType, reach}]-> Post ---

MATCH (u:User {id:'user-003'}), (p:Post {id:'post-003'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-14T19:30:00'), r.shareType='REPOST',   r.reach=320;
MATCH (u:User {id:'user-004'}), (p:Post {id:'post-004'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-14T21:00:00'), r.shareType='REPOST',   r.reach=110;
MATCH (u:User {id:'user-005'}), (p:Post {id:'post-003'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-14T22:15:00'), r.shareType='QUOTE',    r.reach=50000;
MATCH (u:User {id:'user-008'}), (p:Post {id:'post-007'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-19T23:25:00'), r.shareType='REPOST',   r.reach=480;
MATCH (u:User {id:'user-006'}), (p:Post {id:'post-007'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-19T23:30:00'), r.shareType='REPOST',   r.reach=1800;
MATCH (u:User {id:'user-010'}), (p:Post {id:'post-001'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-10T11:00:00'), r.shareType='REACTION', r.reach=240;
MATCH (u:User {id:'user-003'}), (p:Post {id:'post-010'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-14T21:45:00'), r.shareType='REPOST',   r.reach=130;
MATCH (u:User {id:'user-004'}), (p:Post {id:'post-011'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-15T10:00:00'), r.shareType='REPOST',   r.reach=90;
MATCH (u:User {id:'user-008'}), (p:Post {id:'post-006'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-18T22:00:00'), r.shareType='REACTION', r.reach=360;
MATCH (u:User {id:'user-010'}), (p:Post {id:'post-008'}) MERGE (u)-[r:SHARED]->(p) SET r.sharedAt=datetime('2026-03-20T11:00:00'), r.shareType='REPOST',   r.reach=240;


// --- User -[:FOLLOWS {since, interactionStrength}]-> User ---

MATCH (a:User {id:'user-003'}), (b:User {id:'user-002'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-04-01'), r.interactionStrength=0.70;
MATCH (a:User {id:'user-004'}), (b:User {id:'user-002'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-04-22'), r.interactionStrength=0.40;
MATCH (a:User {id:'user-004'}), (b:User {id:'user-005'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-05-10'), r.interactionStrength=0.80;
MATCH (a:User {id:'user-008'}), (b:User {id:'user-001'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-08-10'), r.interactionStrength=0.50;
MATCH (a:User {id:'user-010'}), (b:User {id:'user-002'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-10-05'), r.interactionStrength=0.60;
MATCH (a:User {id:'user-006'}), (b:User {id:'user-005'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-07-20'), r.interactionStrength=0.65;
MATCH (a:User {id:'user-006'}), (b:User {id:'user-009'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2026-01-15'), r.interactionStrength=0.90;
MATCH (a:User {id:'user-009'}), (b:User {id:'user-006'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2026-01-15'), r.interactionStrength=0.90;
MATCH (a:User {id:'user-003'}), (b:User {id:'user-004'}) MERGE (a)-[r:FOLLOWS]->(b) SET r.since=date('2025-05-01'), r.interactionStrength=0.55;


// --- User -[:INTERACTS_WITH {interactionType, weight, lastInteractionAt}]-> User ---

MATCH (a:User {id:'user-001'}), (b:User {id:'user-006'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='REPLY',   r.weight=0.60, r.lastInteractionAt=datetime('2026-03-15T17:00:00');
MATCH (a:User {id:'user-002'}), (b:User {id:'user-005'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='MENTION', r.weight=0.40, r.lastInteractionAt=datetime('2026-03-14T22:00:00');
MATCH (a:User {id:'user-005'}), (b:User {id:'user-006'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='QUOTE',   r.weight=0.70, r.lastInteractionAt=datetime('2026-03-15T08:00:00');
MATCH (a:User {id:'user-008'}), (b:User {id:'user-003'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='REPLY',   r.weight=0.30, r.lastInteractionAt=datetime('2026-03-12T20:00:00');
MATCH (a:User {id:'user-004'}), (b:User {id:'user-010'}) MERGE (a)-[r:INTERACTS_WITH]->(b) SET r.interactionType='MENTION', r.weight=0.50, r.lastInteractionAt=datetime('2026-03-18T19:00:00');


// --- Claim -[:SUPPORTED_BY {confidence, note}]-> Evidence ---

MATCH (c:Claim {id:'claim-001'}), (e:Evidence {id:'evid-001'})
MERGE (c)-[r:SUPPORTED_BY]->(e)
SET r.confidence=0.60, r.note='El reporte oficial respalda parcialmente: registra desaceleración pero no la cifra del 30%.';

MATCH (c:Claim {id:'claim-002'}), (e:Evidence {id:'evid-002'})
MERGE (c)-[r:SUPPORTED_BY]->(e)
SET r.confidence=0.95, r.note='Paper indexado con datos de fase III; respalda la cifra y la metodología.';

MATCH (c:Claim {id:'claim-004'}), (e:Evidence {id:'evid-004'})
MERGE (c)-[r:SUPPORTED_BY]->(e)
SET r.confidence=0.90, r.note='Comunicado de agencia espacial coincide con datos de radar publicados.';

MATCH (c:Claim {id:'claim-005'}), (e:Evidence {id:'evid-005'})
MERGE (c)-[r:SUPPORTED_BY]->(e)
SET r.confidence=0.92, r.note='Serie histórica de temperaturas confirma el valor reportado.';


// --- Claim -[:REFUTED_BY {confidence, note}]-> Evidence ---

MATCH (c:Claim {id:'claim-003'}), (e:Evidence {id:'evid-003'})
MERGE (c)-[r:REFUTED_BY]->(e)
SET r.confidence=0.97, r.note='Posición oficial de la OMS descarta vínculo entre 5G y los síntomas descritos.';

MATCH (c:Claim {id:'claim-006'}), (e:Evidence {id:'evid-001'})
MERGE (c)-[r:REFUTED_BY]->(e)
SET r.confidence=0.85, r.note='Reporte oficial muestra desaceleración, no escenario de colapso.';

MATCH (c:Claim {id:'claim-007'}), (e:Evidence {id:'evid-006'})
MERGE (c)-[r:REFUTED_BY]->(e)
SET r.confidence=0.95, r.note='Estudio atmosférico revisado por pares desmiente la hipótesis de chemtrails.';


// --- FactCheck -[:CHECKS {checkedAt, method}]-> Claim ---

MATCH (f:FactCheck {id:'fc-001'}), (c:Claim {id:'claim-001'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-03-16T12:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-002'}), (c:Claim {id:'claim-002'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-03-13T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-003'}), (c:Claim {id:'claim-003'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-03-15T16:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-004'}), (c:Claim {id:'claim-006'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-03-19T10:00:00'), r.method='MANUAL_REVIEW';
MATCH (f:FactCheck {id:'fc-005'}), (c:Claim {id:'claim-007'}) MERGE (f)-[r:CHECKS]->(c) SET r.checkedAt=datetime('2026-03-20T11:00:00'), r.method='MANUAL_REVIEW';


// --- FactCheck -[:BASED_ON {relevance, usedAs}]-> Evidence ---

MATCH (f:FactCheck {id:'fc-001'}), (e:Evidence {id:'evid-001'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.80, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-002'}), (e:Evidence {id:'evid-002'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.95, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-003'}), (e:Evidence {id:'evid-003'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.97, r.usedAs='PRIMARY';
MATCH (f:FactCheck {id:'fc-004'}), (e:Evidence {id:'evid-001'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.70, r.usedAs='CORROBORATING';
MATCH (f:FactCheck {id:'fc-005'}), (e:Evidence {id:'evid-006'}) MERGE (f)-[r:BASED_ON]->(e) SET r.relevance=0.95, r.usedAs='PRIMARY';


// ============================================================
// 10. Verificaciones rápidas (opcional)
// ============================================================
//
// Conteo de nodos por etiqueta:
//   MATCH (n) RETURN labels(n)[0] AS label, count(*) AS total ORDER BY label;
//
// Conteo de relaciones por tipo:
//   MATCH ()-[r]->() RETURN type(r) AS rel, count(*) AS total ORDER BY rel;
//
// Confirmar que una relación PUBLISHED_BY tiene propiedades:
//   MATCH (n:News)-[r:PUBLISHED_BY]->(s:Source) RETURN n.title, properties(r) LIMIT 3;
