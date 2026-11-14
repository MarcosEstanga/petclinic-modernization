# Definition of Done — mapeada a los criterios de evaluación

Evidencia al **2026-11-14**, commit de la release `v1.0.0`, JDK 21.0.13, Windows 11.

## 1. Análisis del código existente

- [x] `docs/ANALYSIS.md`: modelo ER (Mermaid), endpoints originales (MVC y `/vets` JSON),
  dependencias, **14 hallazgos de deuda técnica**, mapa de bounded contexts y línea base.
- [x] `docs/agent/BUSINESS_RULES.md` validado contra el código: **5 reglas nuevas**
  (BR-OWN-13/14, BR-PET-10/11/12, BR-VIS-05, BR-SYS-05), **1 corregida** (BR-VET-02b) y columna
  `Test` completa (salvo las 4 excepciones justificadas).
- [x] Línea base registrada en `docs/agent/PROGRESS.md`: `Tests run: 79, Failures: 0, Skipped: 2`
  (los 2 *skipped* son Testcontainers sin Docker).

## 2. Migración a Java 21+

- [x] `<java.version>21</java.version>` en el POM padre (`packaging=pom`).
- [x] CI y devcontainer/Gitpod en JDK 21; Gradle eliminado y documentado (`MIGRATION.md` §2.4).
- [x] Features de Java 21 en uso real y listadas en `MIGRATION.md` §2.2: `record`s (`Vets`,
  DTOs), `SequencedCollection#getFirst`, hilos virtuales, pattern matching preexistente.
- [x] Versiones verificadas con fuente en `MIGRATION.md` §1: Boot 4.1.0, Spring Cloud 2025.1.4,
  Gateway 5.0.4, springdoc 3.2.0, checkstyle 13.11.0.

## 3. Arquitectura modular / microservicios

- [x] Multi-módulo Maven: `customers-service`, `vets-service`, `visits-service`, `api-gateway`,
  `libs/common-web` y `tests/parity-tests` (`./mvnw verify` compila los 7 módulos).
- [x] Base de datos por servicio; sin entidades compartidas (`schema.sql` propio por servicio).
- [x] Comunicación visits → customers con `RestClient` + `@HttpExchange`, timeouts 2 s/5 s y
  traducción 404→404, caída→503 (`CustomersGateway`, `CustomersGatewayTests`).
- [x] Capas por servicio: `api` / `application` / `domain` / `infrastructure`.
- [x] `docs/ARCHITECTURE.md` con diagramas Mermaid y ADR-001…ADR-006 (microservicios, gateway,
  BD por servicio, paginación, errores, descubrimiento).

## 4. REST + OpenAPI

- [x] springdoc en cada servicio: `/v3/api-docs` y `/swagger-ui.html` (verificado en
  `CustomersServiceIntegrationTests#openApiDocumentAndSwaggerUiArePublished` y equivalentes).
- [x] Swagger agregado en el gateway (`springdoc.swagger-ui.urls[0..3]`, una pestaña por servicio
  y otra para el gateway).
- [x] DTOs `record` con `@Schema` y controladores con `@Operation`/`@ApiResponse`.
- [x] Errores con `ProblemDetail`; códigos correctos: 201 + `Location`, 400, 404, 409, 503
  (`ProblemDetailExceptionHandler`, tests de cada servicio).
- [x] `docs/API.md` con tablas, ejemplos y modelo de errores; `docs/http/*.http` con flujo E2E.

## 5. Pruebas automatizadas y preservación funcional

- [x] Un test por regla de `BUSINESS_RULES.md` nombrado con su ID; **verificado por
  `ParityTraceabilityTests`** (falla el build si falta).
- [x] Unit + `@WebMvcTest` + `@DataJpaTest` + `@SpringBootTest` en los tres servicios.
- [x] Test del cliente HTTP inter-servicio con `MockRestServiceServer` (`CustomersGatewayTests`).
- [x] `./mvnw verify` verde **desde un clon limpio**: ver `PROGRESS.md` (smoke test).
- [x] Cobertura JaCoCo reportada en `docs/TESTING.md`: 93,3 % / 96,6 % / 96,6 % / 97,3 % de líneas.
- [x] Tabla de trazabilidad regla → test en `docs/TESTING.md`.

## 6. Documentación técnica (README raíz)

- [x] Requisitos (JDK 21, Docker opcional) y quick start en ≤ 5 comandos.
- [x] Compilar, ejecutar cada servicio (`./mvnw -pl … spring-boot:run`) y con Docker Compose.
- [x] Puertos, URLs de Swagger, perfiles y variables de entorno (`CUSTOMERS_SERVICE_URL`, …).
- [x] Cómo correr tests y ver cobertura.
- [x] Estructura del repositorio y enlaces a `docs/*`.
- [x] Estrategia de ramas (GitFlow) y cómo contribuir.
- [x] Limitaciones conocidas y trabajo futuro.

## 7. Uso de IA

- [x] `docs/AI_USAGE.md` con herramientas y entradas por fase (prompt, resultado, validación,
  correcciones).
- [x] Sección «Cómo se validó lo generado por IA» y tabla resumen de **17 errores detectados y
  corregidos**.
- [x] `AGENTS.md`, `CLAUDE.md`, `opencode.json` y `docs/agent/*` conservados como evidencia del
  meta-prompting.

## 8. Git / GitFlow

- [x] Historial upstream preservado (`git log` contiene los commits originales hasta `500158f`).
- [x] Ramas `main`, `develop`, `feature/analysis`, `feature/java21-migration`,
  `feature/multimodule-skeleton`, `feature/customers-service`, `feature/vets-service`,
  `feature/visits-service`, `feature/api-gateway`, `feature/parity-tests-and-docker`,
  `feature/documentation` y `release/1.0.0`, todas con merge `--no-ff`.
- [x] Tag anotado `v1.0.0` (y `legacy-java21` con el monolito migrado).
- [x] Conventional Commits en todo el historial propio.
- [x] Repo **público**: https://github.com/MarcosEstanga/petclinic-modernization (CI `43618514746`
  en verde sobre `main`, JDK 21).
- [x] Prueba de humo desde clon limpio: `scripts/smoke-test.ps1` clona el repo, compila, arranca los
  cuatro servicios, espera `/actuator/health`, ejecuta búsqueda, `vets/all`, composición y un POST de
  visita, y comprueba Swagger UI → **SMOKE TEST OK**.
