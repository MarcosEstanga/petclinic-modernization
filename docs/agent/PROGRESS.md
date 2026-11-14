# Progreso (lo actualiza el agente al cerrar cada fase)

| Fase | Rama | Estado | Verificación (comando → resultado) | Notas |
|---|---|---|---|---|
| 0 Setup y análisis | feature/analysis | ✅ hecho | `.\mvnw.cmd -B test` → `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2` · `BUILD SUCCESS` (1:25 min) | Línea base con JDK 17. `docs/ANALYSIS.md` creado; `BUSINESS_RULES.md` validado (+5 reglas, 1 corregida) |
| 1 Java 21 | feature/java21-migration | ✅ hecho | `.\mvnw.cmd -B verify` (JDK 21.0.13) → `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2` · `BUILD SUCCESS` (52 s) · checkstyle: `0 Checkstyle violations` | `java.version=21`, records (`Vets`), `getFirst()` (SequencedCollection), hilos virtuales, checks de CI y Gradle a 21, JAXB muerto eliminado. Tag `legacy-java21` |
| 2 Esqueleto multi-módulo | feature/multimodule-skeleton | ✅ hecho | `.\mvnw.cmd -B verify` → 6 módulos `SUCCESS`, 4 tests de contexto, `BUILD SUCCESS` (53 s) | Parent `packaging=pom`, `libs/common-web`, 3 servicios + gateway; monolito retirado a tag `legacy-java21` |
| 3 customers-service | feature/customers-service | ✅ hecho | `.\mvnw.cmd -B verify` → `Tests run: 55` en el módulo (58 en total), `BUILD SUCCESS` | Owners/pets/petTypes REST con ProblemDetail, OpenAPI, 409 en duplicados, paginación ADR-004 y tests de paridad BR-OWN/BR-PET |
| 4 vets-service | feature/vets-service | ✅ hecho | `.\mvnw.cmd -B -pl services/vets-service -am test` → `Tests run: 16, Failures: 0`, `BUILD SUCCESS` | REST paginado + `/vets/all` con contrato `vetList`, caché Caffeine (`BR-VET-04` verificada con Mockito), seeds y OpenAPI |
| 5 visits-service | feature/visits-service | ✅ hecho | `.\mvnw.cmd -B -pl services/visits-service -am test` → `Tests run: 29, Failures: 0`, `BUILD SUCCESS` | Cliente `@HttpExchange` + `RestClient` con timeouts, 404/503 remotos, fecha futura y bulk `/pets/visits` |
| 6 api-gateway | feature/api-gateway | ✅ hecho | `.\mvnw.cmd -B -pl api-gateway -am test` → `Tests run: 13, Failures: 0`, `BUILD SUCCESS` | Rutas `/api/customers`, `/api/vets`, `/api/visits` (Gateway MVC 5.0.4), composición `/api/owners/{id}/details`, Swagger agregado y `/api/system/boom` |
| 7 Paridad, calidad, Docker, CI | feature/parity-tests-and-docker | ✅ hecho | `.\mvnw.cmd -B verify` → **117 tests, 0 fallos**, 7 módulos `SUCCESS`; `ParityTraceabilityTests` 2/2 | JaCoCo: customers 93,3 %, vets 96,6 %, visits 96,6 %, gateway 97,3 % de líneas. `Dockerfile` + `docker-compose.yml` + `ci.yml` (JDK 21) |
| 8 Documentación | feature/documentation | ✅ hecho | `./mvnw -B verify` sigue verde tras los cambios de docs (ver Fase 7) | README reescrito, ARCHITECTURE con ADRs y contenedores, API.md, TESTING.md, MIGRATION.md, AI_USAGE.md y `docs/http/*.http` |
| 9 Release y publicación | release/1.0.0 | ✅ release local (publicación pendiente) | `./mvnw -B verify` → 117 tests, `BUILD SUCCESS` con versión `1.0.0`; `scripts/smoke-test.ps1` → **SMOKE TEST OK** desde clon limpio | Tag `v1.0.0`; merge `--no-ff` a `main` y `develop`. No se publica en GitHub por decisión del usuario |

## Línea base del monolito original

**Commit `500158f`**, Spring Boot `4.1.0`, `java.version=17`, Temurin 17.0.20, sin Docker.

```
.\mvnw.cmd -B test
[INFO] Tests run: 79, Failures: 0, Errors: 0, Skipped: 2
[INFO] BUILD SUCCESS
[INFO] Total time:  01:25 min
```

- 2 *skipped*: `MySqlIntegrationTests` y `PostgresIntegrationTests` (Testcontainers se
  auto-desactivan sin Docker, comportamiento esperado).
- Clases de test: 18. Ver desglose en `docs/ANALYSIS.md` §7.

## Entorno

- Windows 11, PowerShell. `JAVA_HOME` del sistema apuntaba a un JDK inexistente
  (`jdk-17.0.8.101-hotspot`); se corrige por sesión.
- **JDK 21**: `winget install EclipseAdoptium.Temurin.21.JDK` se quedó esperando (UAC); se instaló
  sin privilegios descargando el zip oficial de Adoptium en `C:\Users\marci\.jdks\jdk-21.0.13+9`.
  `mvnw` usa `JAVA_HOME`, así que el build corre en 21 aunque `java` del `PATH` siga siendo 17.
- `gh` CLI **sin autenticar**: la publicación del repo (Fase 9) requiere `gh auth login`.
- `origin` (upstream de Spring) renombrado a `upstream`; `develop` creada desde `main`.

## Bloqueos / decisiones pendientes del usuario

- **Publicación en GitHub**: el usuario decidió no publicar por ahora (2026-11-14). Todo está listo
  para `gh repo create <usuario>/petclinic-modernization --public --source . --remote origin` y el
  push de ramas/tags (ver §«Publicación»).

## Prueba de humo desde clon limpio (Fase 9)

```
== 1/5 Cloning the repository into C:\Users\marci\AppData\Local\Temp\petclinic-smoke-20261114122816 ==
== 2/5 Building (tests already ran in verify) ==
== 3/5 Starting the services ==
   customers -> pid 29904 (:8081)
   vets -> pid 30116 (:8083)
   visits -> pid 23052 (:8082)
   gateway -> pid 12812 (:8080)
== 4/5 Waiting for /actuator/health ==
   customers UP
   vets UP
   visits UP
   gateway UP
== 5/5 End-to-end calls ==
   owners?lastName=fran -> Franklin (total 1)
   vets/all -> 6 vets
   owners/6/details -> Coleman, 2 pets, 4 visits
   POST visit -> id 5 for 2026-11-24
   swagger-ui -> HTTP 200 (following redirects)

SMOKE TEST OK
```

Script reproducible: [`scripts/smoke-test.ps1`](../../scripts/smoke-test.ps1)
(`powershell -File scripts/smoke-test.ps1 -JavaHome <JDK21>`).

## Publicación (pendiente de confirmación)

```bash
# 1. Autenticarse una sola vez
gh auth login

# 2. Crear el repositorio público y añadirlo como origin
gh repo create <usuario>/petclinic-modernization --public --source . --remote origin

# 3. Subir todo el historial, todas las ramas y los tags
git push -u origin --all
git push origin --tags

# 4. Comprobar la CI y el tag
gh run list --limit 5
gh run watch
gh release create v1.0.0 --title "v1.0.0" --notes-file CHANGELOG.md
```

Después, actualizar el badge del README y el enlace `[1.0.0]` del `CHANGELOG.md` con la URL real.
