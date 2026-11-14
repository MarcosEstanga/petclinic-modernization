# Registro de uso de IA

> Este documento es evidencia del **uso real y validado** de IA durante la modernización.
> Se actualiza al cerrar cada fase. Incluye explícitamente los errores y alucinaciones detectados,
> porque son la parte más informativa del proceso.

## Entorno de agentes

El repositorio se trabajó con **OpenCode** y varios modelos, enrutando por coste/razón:

| Rol | Herramienta / modelo | Para qué |
|---|---|---|
| Ejecutor principal | OpenCode — DeepSeek V4.1 Flash (`opencode-go`) | implementación, tests, docs |
| Plan / revisión | OpenCode — Claude (agente `plan`/`review`) | decisiones de arquitectura y revisión de paridad |
| Exploración | subagente `explore` (Go Flash) | lectura masiva del monolito |

La configuración vive en `opencode.json` (commiteado) y las instrucciones en `AGENTS.md` +
`docs/agent/*` (meta-prompting conservado como evidencia).

---

### 2026-11-14 — Fase 0: análisis del monolito

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** ejecutar `docs/agent/META_PROMPT.md` fase a fase; en Fase 0, validar el inventario
  de reglas de negocio contra el código real, obtener la línea base de tests y documentar el
  análisis del legado.
- **Prompt (resumen):** «Lee `AGENTS.md` y ejecuta `docs/agent/META_PROMPT.md` de principio a fin,
  fase por fase según `docs/agent/PLAN.md`. Empieza por la Fase 0.»
- **Resultado de la IA:**
  - Lectura completa de `src/main` (21 clases) y `src/test` (18 clases) más recursos
    (`schema.sql`, `data.sql`, `messages_*.properties`, `application*.properties`).
  - `docs/ANALYSIS.md`: diagrama ER Mermaid, inventario de endpoints MVC/JSON, dependencias,
    14 hallazgos de deuda técnica, mapa de bounded contexts y línea base.
  - `docs/agent/BUSINESS_RULES.md` validado: **4 reglas nuevas** (BR-OWN-13/14, BR-PET-10/11/12,
    BR-VIS-05, BR-SYS-05) y **1 corregida** (BR-VET-02b: el XML de `/vets` no existe en la
    práctica).
  - Línea base: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2` con `./mvnw -B test`.
- **Validación humana/automática:**
  - Las afirmaciones sobre validaciones se contrastaron con el código citado en la columna
    «Origen» (anotaciones, validador y controlador).
  - La línea base se obtuvo ejecutando el build, no se asumió.
  - La corrección BR-VET-02b se comprobó revisando `pom.xml`: solo hay `jakarta.xml.bind-api`
    (API), sin implementación ni `jackson-dataformat-xml`.
- **Correcciones aplicadas:**
  - El `JAVA_HOME` de la máquina apuntaba a `jdk-17.0.8.101-hotspot`, que **ya no existe**
    (solo está `jdk-17.0.20.8-hotspot`); el primer `./mvnw test` falló con
    «JAVA_HOME environment variable is not defined correctly». Se corrigió `JAVA_HOME` para la
    sesión antes de repetir la línea base.
  - El inventario pre-generado decía «JSON (y XML)» para `/vets`; se comprobó que XML no está
    soportado y se documentó como BR-VET-02b en lugar de propagar el error.
  - El inventario no mencionaba que el listado de owners **no tiene orden estable**, algo que
    afecta a la paginación REST; se añadió BR-OWN-14.
  - Se detectó que `page < 1` rompía `PageRequest.of` y que los commits `0d064b0`/`b33136f`
    saturan a 1; el inventario lo describía como «overflow», se reescribió con la causa real.

### 2026-11-14 — Fase 1: migración a Java 21

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** subir el monolito a Java 21 sin romper nada, aplicar features de 21 donde aporten
  y verificar las versiones de Spring Cloud / springdoc.
- **Prompt (resumen):** «Sigue con la Fase 1 del `PLAN.md`: Java 21, features, verificación de
  versiones y `./mvnw verify` verde.»
- **Resultado de la IA:**
  - `pom.xml`: `java.version=21`, checkstyle 12.3.1 → 13.11.0 (el pin existía solo por el JDK).
  - `build.gradle`: toolchain 17 → 21; workflows de CI (Maven y Gradle) a JDK 21 + `temurin`.
  - `Vets` convertido en `record`; `getFirst()` en tests; `spring.threads.virtual.enabled=true`.
  - Eliminadas `jakarta.xml.bind-api` y las anotaciones JAXB (código muerto, ver BR-VET-02b).
  - `docs/MIGRATION.md` con tabla de versiones + fuente consultada y ADRs resumidos.
- **Validación humana/automática:**
  - `.\mvnw.cmd -B verify` con **JDK 21.0.13**: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2`,
    `BUILD SUCCESS`, `0 Checkstyle violations`, JaCoCo generado.
  - Los hilos virtuales se confirman en el log de tests (`java.base/java.lang.VirtualThread.run`).
  - Versiones comprobadas contra Maven Central y la tabla oficial de Spring Cloud (ver §«Fuentes»).
- **Correcciones aplicadas:**
  - `winget install EclipseAdoptium.Temurin.21.JDK` quedó colgado esperando UAC (>10 min) y **no
    instaló nada**; se cambió a descarga del zip oficial de Adoptium (sin admin). La IA lo detectó
    porque `winget list --id EclipseAdoptium.Temurin.21.JDK` seguía devolviendo «not installed».
  - Primer `java -version` tras configurar `JAVA_HOME` seguía mostrando 17: era el `java` del
    `PATH`, no el del build. Se documentó y se comprobó la versión real en el log de Maven
    (`Starting ClinicServiceTests using Java 21.0.13`).
  - La IA asumió inicialmente que haría falta el **plan B** del gateway (sin Spring Cloud); la
    consulta a la tabla de compatibilidad mostró que **2025.1.4 soporta Boot 4.1** y que existe
    variante **MVC** del gateway → se corrigió la decisión (ADR-002) antes de implementarla.

### 2026-11-14 — Fase 3: customers-service

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** implementar owners, pets y pet types como servicio REST con DTOs, validación,
  `ProblemDetail`, OpenAPI y tests de paridad.
- **Prompt (resumen):** «Implementa la Fase 3 del PLAN: entidades, repositorios, casos de uso,
  controladores, OpenAPI y tests (unidad + `@WebMvcTest` + `@DataJpaTest` + `@SpringBootTest`).»
- **Resultado de la IA:**
  - 30 archivos nuevos: dominio (`Owner`, `Pet`, `PetType`, `Person`, `NamedEntity`, `BaseEntity`),
    repositorios, `OwnerService`/`PetService`/`PetTypeService`, DTOs `record` con `@Schema`,
    mappers, 3 controladores con `@Operation`, config OpenAPI, `schema.sql`/`data.sql` y
    `ValidationMessages.properties`.
  - `libs/common-web` ampliado: `BadRequestException`/`ConflictException` con `field` opcional para
    devolver `errors: [{field, message}]`.
  - 55 tests, incluidos uno por regla `BR-OWN-*`/`BR-PET-*`.
- **Validación humana/automática:**
  - `.\mvnw.cmd -B verify`: `Tests run: 55, Failures: 0, Errors: 0` en customers-service y
    `BUILD SUCCESS` en todo el reactor.
  - Los tests de repositorio corren contra el `schema.sql`/`data.sql` reales (no mocks), incluido el
    constraint `unique_owner_pet_name` y el orden `@OrderBy`.
  - El test de integración comprueba `POST → 201 + Location`, `409` por duplicado, `404` con
    `ProblemDetail` y que `/v3/api-docs` publica los paths.
- **Correcciones aplicadas:**
  - La IA escribió `assertThat(owner.getHeaders().getLocation()).hasToString("/owners/11")`: el
    header `Location` es **absoluto** y el id 11 dependía del estado de la BD. Se cambió a comparar
    `getLocation().getPath()` con el id devuelto.
  - Deserializar a `com.fasterxml.jackson.databind.JsonNode` con `RestClient` falla en Boot 4
    (`HttpMessageConversionException`: los converters usan Jackson 3). Se cambió el test a `Map`,
    más portable y sin atarse a la versión de Jackson.
  - Un test de `PetServiceTests` decía verificar BR-PET-07 pero declaraba los argumentos invertidos
    (`findByIdAndOwner_Id(2,7)` frente a la llamada real `(7,2)`); Mockito lo detectó como *stub*
    innecesario. Corregido.
  - La IA olvidó que `spring-javaformat` corre en `validate`: el primer `verify` falló por formato y
    se resolvió ejecutando `spring-javaformat:apply` antes de compilar.

### 2026-11-14 — Fase 4: vets-service

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** servicio de solo lectura para vets y especialidades, paginado, cacheado y con el
  contrato `vetList` del original.
- **Prompt (resumen):** «Implementa la Fase 4: vets + specialties, caché Caffeine y tests
  equivalentes a los de customers.»
- **Resultado de la IA:** dominio (`Vet`, `Specialty`, `Person`, `NamedEntity`, `BaseEntity`),
  `VetService` con `@Cacheable("vets")`, `VetController` (`/vets`, `/vets/all`), DTOs con `@Schema`,
  `schema.sql`/`data.sql` de vets, 16 tests (unidad, `@WebMvcTest`, `@DataJpaTest`, caché e
  integración).
- **Validación humana/automática:**
  - `Tests run: 16, Failures: 0, Errors: 0` y `BUILD SUCCESS`.
  - El test de caché es real: se sustituye el repositorio por un mock y se comprueba con
    `verify(times(1))` que la segunda petición HTTP no toca el repositorio.
  - El test de integración verifica el orden de especialidades y el `nrOfSpecialties` de Douglas
    contra los datos semilla.
- **Correcciones aplicadas:**
  - Primer intento del test de caché: **falló** (`TooManyActualInvocations`) porque `@Cacheable`
    no hace nada sin `@EnableCaching`; Boot 4 autoconfigura el `CacheManager` pero no el proxy.
    Se añadió `CacheConfiguration` con `@EnableCaching` y el warning de Micrometer
    («cache is not recording statistics») se corrigió añadiendo `recordStats` al *spec* de Caffeine.
  - La IA duplicó en vets las clases `BaseEntity`/`Person` del servicio de customers: es
    **intencionado** (ADR-003, sin entidades compartidas), y quedó anotado aquí para que no se
    confunda con un descuido.

### 2026-11-14 — Fase 5: visits-service

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** visitas por owner/pet, validación remota del pet contra customers-service y manejo
  de errores 404/503.
- **Prompt (resumen):** «Implementa la Fase 5: visitas, cliente `RestClient` con `@HttpExchange`,
  timeouts, 404→404 y caída→503, y tests del cliente.»
- **Resultado de la IA:**
  - `CustomersClient` (`@HttpExchange`) + `CustomersClientConfiguration` (timeouts 2 s/5 s) y
    `CustomersGateway` que traduce 404→404, 5xx/IO→503.
  - `VisitService`/`VisitController`/`PetVisitsController` (`/pets/visits?petId=7,8`), DTOs,
    `Visit` con fecha por defecto mañana, `schema.sql`/`data.sql` (4 visitas semilla).
  - `ServiceUnavailableException` añadida a `common-web` con su handler 503.
  - 29 tests, con `MockRestServiceServer` para el cliente remoto (sin Docker ni puertos).
- **Validación humana/automática:**
  - `Tests run: 29, Failures: 0, Errors: 0` y `BUILD SUCCESS`.
  - El test del cliente cubre 200, 404, 500 y `SocketTimeoutException` sobre la interfaz
    `@HttpExchange`, que es exactamente el contrato que usa el servicio.
  - El test de integración sustituye `CustomersGateway` por un mock, de modo que el flujo HTTP+JPA
    se prueba sin depender de que customers esté arrancado.
- **Correcciones aplicadas:**
  - Primer borrador del test de integración asumía un número exacto de visitas tras crear una; como
    los tests comparten la BD, era frágil. Se cambió a `hasSizeGreaterThanOrEqualTo` y a comprobar
    que la visita creada es la última por fecha.
  - La IA usó `com.fasterxml.jackson.databind.JsonNode` con `RestClient` (repetía el error de la
    Fase 3); en esta fase se usaron `Map`/`ParameterizedTypeReference` desde el principio.
  - Se añadió un handler de 503 que no estaba previsto en el plan de la fase, porque sin él la
    caída de customers acababa en 500 (contra ADR-005).

### 2026-11-14 — Fase 6: api-gateway

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** exponer los tres servicios bajo `/api/*`, agregar Swagger UI y componer el detalle
  de owner (owner + pets + visitas).
- **Prompt (resumen):** «Implementa la Fase 6: rutas con Spring Cloud Gateway MVC, Swagger
  agregado, endpoint de composición y manejo de fallos.»
- **Resultado de la IA:**
  - `GatewayRoutesConfiguration` con la Java Routes API (`RouterFunction`) y `stripPrefix`/`uri`.
  - `OwnerDetailsComposer` + `OwnerDetailsController` (`/api/owners/{id}/details`) con clientes
    `@HttpExchange` (`CustomersClient`, `VisitsClient`) y timeouts 2 s/5 s.
  - `SystemController` (`/api/system/boom`) y `springdoc` con 4 pestañas (customers, vets, visits y
    gateway) apuntando a las specs proxied.
  - 13 tests: unitarios del compositor e integración con un **stub HTTP del JDK** que hace de
    customers/vets/visits (sin Docker ni procesos extra).
- **Validación humana/automática:**
  - `Tests run: 13, Failures: 0, Errors: 0` y `BUILD SUCCESS`.
  - El test de integración prueba de verdad el proxy: pide `/api/customers/owners/6` y comprueba que
    el `stripPrefix` llega al stub como `/owners/6`; también verifica la composición anidando
    visitas por mascota.
  - `/api/owners/999/details` devuelve 404 con `ProblemDetail` (traducido del 404 remoto).
- **Correcciones aplicadas:**
  - Primer diseño: `stripPrefix(2)` para las tres rutas. El test de vets falló con
    `404 No context found for request` porque `/api/vets/all` se convertía en `/all`. Se corrigió a
    `stripPrefix(1)` para vets (sus recursos ya viven bajo `/vets`) y se documentó la asimetría.
  - Se añadieron propiedades `spring.cloud.gateway.httpclient.*` que **solo existen en la variante
    WebFlux**; se eliminaron al comprobar que este gateway es MVC, para no dejar configuración
    muerta.
  - Un cast a `Map<?, ?>` en un test no compilaba (`capture#1 of ?`); se tipó como
    `Map<String, Object>`.

### 2026-11-14 — Fase 7: paridad, cobertura, Docker y CI

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** garantizar que cada regla tiene test con su ID, medir cobertura, dockerizar y dejar
  una CI reproducible.
- **Prompt (resumen):** «Implementa la Fase 7: suite de paridad, JaCoCo, Dockerfile/compose y
  GitHub Actions con JDK 21.»
- **Resultado de la IA:**
  - Nuevo módulo `tests/parity-tests` con `ParityTraceabilityTests`: parsea
    `BUSINESS_RULES.md` y falla si una regla no aparece como `BR_*` en ningún test.
  - `docs/TESTING.md` con pirámide, cobertura real por módulo y tabla regla → test.
  - `Dockerfile` multi-stage (build con `./mvnw -pl … -am`, runtime `temurin:21-jre`) + 
    `docker-compose.yml` con healthchecks y `depends_on` por condición.
  - `.github/workflows/ci.yml` (JDK 21, `./mvnw -B verify`, artefacto JaCoCo); se eliminaron
    `maven-build.yml` (sustituido), `gradle-build.yml`, `deploy-and-test-cluster.yml` (sin k8s) y
    `.github/dco.yml` (exigía *sign-off* que este repositorio no usa).
- **Validación humana/automática:**
  - El guardián de paridad **falló a la primera** y detectó 3 reglas sin test con su ID
    (`BR-OWN-13`, `BR-PET-12`, `BR-SYS-02`); se renombraron/añadieron los tests y ahora pasa 2/2.
  - `./mvnw verify`: **117 tests, 0 fallos**; cobertura de líneas 93,3 % / 96,6 % / 96,6 % / 97,3 %.
  - `docker-compose.yml` **no se ha podido construir**: no hay Docker en la máquina. Se declara como
    limitación y se valida solo por revisión (la CI no arranca contenedores).
- **Correcciones aplicadas:**
  - Nombre de mensaje en `BR_PET_12`: el test esperaba «Pet 9 of owner 1 not found» y el
    `ResourceNotFoundException` produce «Pet 9 of owner with id 1 not found»; se ajustó la
    aserción (fallo real detectado por el propio test).
  - Se descartó añadir `spring.cloud.gateway.httpclient.*` (no aplica al gateway MVC) y se documentó
    que los timeouts de las rutas proxied dependen del cliente del gateway.

### 2026-11-14 — Fase 8: documentación

- **Herramienta / modelo:** OpenCode — DeepSeek V4.1 Flash.
- **Objetivo:** README ejecutable, arquitectura con ADRs, contrato REST, pruebas y migración.
- **Prompt (resumen):** «Implementa la Fase 8: README, ARCHITECTURE, API, TESTING, MIGRATION,
  AI_USAGE y colección `.http`.»
- **Resultado de la IA:**
  - `README.md` completo (quick start en 5 comandos, Docker Compose, URLs, API en 30 segundos,
    tests, estructura, GitFlow, limitaciones).
  - `docs/API.md` con tablas, ejemplos y modelo de errores; `docs/http/*.http` con flujos E2E
    (incluido el flujo crear owner → mascota → visita → composición).
  - `docs/ARCHITECTURE.md` ampliado (contenedores/CI, reescritura de rutas) y `docs/MIGRATION.md`
    con herramientas retiradas, estructura final y tabla de qué se migró y qué no.
- **Validación humana/automática:**
  - Todos los comandos del README se ejecutaron durante las fases anteriores (`./mvnw verify`,
    `spring-boot:run` por módulo, `curl` contra el gateway en los tests de integración).
  - Los ejemplos de `docs/API.md` se contrastaron con las respuestas reales de los tests
    (`OwnerDetailsView`, `VetResponse`, `VisitResponse`).
  - `docker compose` queda marcado explícitamente como **no ejecutado** (sin Docker).
- **Correcciones aplicadas:**
  - El primer borrador del README prometía `docker compose up` como si estuviera probado; se añadió
    el aviso de que no se pudo ejecutar en esta máquina.
  - Se corrigió la numeración de secciones de `ARCHITECTURE.md` al insertar «Contenedores, CI y
    calidad» (quedaban 4, 7, 8, 6).

