# Plan de ejecución por fases

Presupuesto total: **1 día (~8–10 h de agente)**. Los tiempos son orientativos.

## GitFlow

```
main ──●──────────────────────────────────────────●── (v1.0.0)
        \                                        /
develop  ●──●──────●──────●──────●──────●──────●
             \    / \    / \    / \    /  \   /
            feature/*   ...    ...   ...  release/1.0.0
```

- `main`: solo releases (y el historial upstream original).
- `develop`: integración. Se crea desde `main` al inicio.
- `feature/<nombre>`: desde `develop`, merge a `develop` con `git merge --no-ff`.
- `release/1.0.0`: desde `develop` al final; solo docs/fixes/bump de versión → merge `--no-ff`
  a `main` **y** a `develop`, tag anotado `v1.0.0`.
- `hotfix/*`: solo si aparece un bug tras la release (desde `main`, merge a `main` y `develop`).
- Push de **todas** las ramas (`git push -u origin --all` y `--tags`) para que el evaluador vea la estrategia.
- Si el repo remoto tiene permisos, opcional: abrir PRs con `gh pr create` por feature y mergearlos
  (deja trazabilidad visible en GitHub). Si no, merges locales `--no-ff` son suficientes.

---

## Fase 0 — Setup y análisis (≈45 min) · rama `feature/analysis`

1. **Pide confirmación** e instala JDK 21 si falta:
   `winget install EclipseAdoptium.Temurin.21.JDK` → configurar `JAVA_HOME` para la sesión.
   Verifica con `java -version`.
2. Git:
   ```bash
   git remote rename origin upstream
   git checkout -b develop
   git checkout -b feature/analysis
   ```
3. Ejecuta los tests del monolito **tal cual** (`./mvnw -q test`) y guarda el resultado como
   línea base en `PROGRESS.md` (nº tests, fallos).
4. Lee todo `src/main` y `src/test`. Valida y completa `docs/agent/BUSINESS_RULES.md`.
5. Crea `docs/ANALYSIS.md`: modelo de dominio (diagrama ER en Mermaid), endpoints existentes
   (MVC y `/vets` JSON/XML), dependencias, deuda técnica observada, mapa de bounded contexts.
6. Commit(s) `docs: analysis of legacy petclinic` → merge a `develop`.

## Fase 1 — Migración a Java 21 del monolito (≈45 min) · `feature/java21-migration`

Objetivo: demostrar migración tecnológica sobre el código original antes de partirlo.

1. `java.version` → `21`; ajustar `build.gradle` (toolchain 21) si se mantiene Gradle;
   CI y devcontainer a 21. Revisar checkstyle (el comentario del pom dice v13 requiere JDK 21: ahora se puede subir).
2. Aplicar features de Java 21 **donde aporten** (no por adorno): `record` para DTOs/value objects,
   pattern matching `instanceof`, `switch` expressions, `List.getFirst()`/`SequencedCollection`,
   text blocks en tests. Opcional: virtual threads `spring.threads.virtual.enabled=true`.
3. Verificar versiones de Spring Cloud / springdoc (META_PROMPT §6) y documentar en `docs/MIGRATION.md`.
4. `./mvnw verify` verde con el mismo nº de tests que la línea base (o más).
5. Mover el monolito a `legacy/petclinic-monolith/` **solo si** no rompe el build; si complica,
   déjalo en su rama/tag (`legacy-java21`) y documenta. Recomendado: tag `legacy-java21`
   apuntando al último commit del monolito migrado, y luego en la Fase 2 reestructurar.

## Fase 2 — Esqueleto multi-módulo (≈45 min) · `feature/multimodule-skeleton`

Estructura de `ARCHITECTURE_TARGET.md`. POM padre (`packaging=pom`) con `dependencyManagement`
(Spring Boot BOM, Spring Cloud BOM si aplica, springdoc), plugins comunes (jacoco, spring-javaformat).
Cada servicio arranca vacío con actuator y su puerto. `./mvnw verify` verde.

## Fase 3 — `customers-service` (≈1.5 h) · `feature/customers-service`

Owners, Pets, PetTypes. Entidades JPA, repositorios, servicio de aplicación, controladores REST
con DTOs (`record`) + Bean Validation, `@RestControllerAdvice` con `ProblemDetail` (RFC 9457),
schema/data H2 portados del original. OpenAPI con `@Operation`/`@Schema` y ejemplos.
Tests: unidad del validador de pets, `@WebMvcTest` de cada endpoint (200/201/400/404/409),
`@DataJpaTest` del repositorio (búsqueda por prefijo de apellido, case-insensitive, paginación),
`@SpringBootTest` de integración.

## Fase 4 — `vets-service` (≈45 min) · `feature/vets-service`

Vets + Specialties, solo lectura, paginado y listado completo; caché (Caffeine/JCache como el original).
Mantener soporte JSON (y XML si es barato, el original lo ofrece en `/vets`). Tests equivalentes.

## Fase 5 — `visits-service` (≈1 h) · `feature/visits-service`

Visitas por `petId`. Antes de crear, valida que el pet existe **y pertenece al owner** llamando a
`customers-service` (`RestClient` con interfaz `@HttpExchange`). Manejo de errores remotos
(404 → 404, caída → 503). Test de integración del cliente con WireMock o `MockRestServiceServer`.
Regla de fecha de visita (estrictamente futura) y descripción ≤255.

## Fase 6 — `api-gateway` (≈1 h) · `feature/api-gateway`

Enrutado `/api/customers/**`, `/api/vets/**`, `/api/visits/**`. Swagger UI agregado (springdoc
`urls` apuntando a cada `/v3/api-docs`). Endpoint de composición
`GET /api/owners/{id}/details` (owner + pets + visits) como en la vista `ownerDetails` original.
Timeouts y manejo de fallos (Resilience4j si Spring Cloud está disponible).

## Fase 7 — Paridad, calidad y contenedores (≈1 h) · `feature/parity-tests-and-docker`

- Suite de **tests de paridad**: un test por fila de `BUSINESS_RULES.md`, nombrado con su ID
  (`BR_OWN_03_telephoneMustHave10Digits`). Tabla de trazabilidad regla → test en `docs/TESTING.md`.
- JaCoCo por módulo; reportar cobertura en `docs/TESTING.md` (objetivo ≥ 80% en dominio/servicio).
- `Dockerfile` por servicio (multi-stage, JRE 21) o `spring-boot:build-image`; `docker-compose.yml`
  raíz. Como no hay Docker local, valida sintaxis y deja documentado que se probó en CI.
- GitHub Actions: `.github/workflows/ci.yml` (JDK 21, `./mvnw -B verify`). Revisa/ajusta los
  workflows existentes del upstream para que no fallen (desactivar/eliminar los que no apliquen, documentando).

## Fase 8 — Documentación (≈45 min) · `feature/documentation`

`README.md` reescrito (ver `CHECKLIST.md` §Docs), `docs/ARCHITECTURE.md`, `docs/API.md`,
`docs/TESTING.md`, `docs/MIGRATION.md`, `docs/AI_USAGE.md` completo, `docs/http/*.http` con
flujos de prueba de punta a punta.

## Fase 9 — Release y publicación (≈30 min)

1. `git checkout -b release/1.0.0 develop`; bump versión a `1.0.0`; `CHANGELOG.md`.
2. Merge `--no-ff` a `main` y a `develop`; `git tag -a v1.0.0 -m "..."`.
3. **Pide confirmación** y crea el repo: `gh repo create <usuario>/petclinic-modernization --public --source . --remote origin`.
4. `git push -u origin --all ; git push origin --tags`.
5. Comprueba que el CI pasa en GitHub (`gh run list`, `gh run watch`).
6. Prueba de humo desde un clon limpio (META_PROMPT §7).

## Recorte (si falta tiempo, se cae en este orden)

1. XML en vets · 2. Resilience4j · 3. Dockerfiles por servicio (dejar solo compose con build-image)
4. Endpoint de composición en gateway · 5. Mover monolito a `legacy/`.
**Nunca recortar**: Java 21, tests de paridad, OpenAPI, README ejecutable, AI_USAGE, GitFlow.
