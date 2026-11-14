# Progreso (lo actualiza el agente al cerrar cada fase)

| Fase | Rama | Estado | Verificación (comando → resultado) | Notas |
|---|---|---|---|---|
| 0 Setup y análisis | feature/analysis | ✅ hecho | `.\mvnw.cmd -B test` → `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2` · `BUILD SUCCESS` (1:25 min) | Línea base con JDK 17. `docs/ANALYSIS.md` creado; `BUSINESS_RULES.md` validado (+5 reglas, 1 corregida) |
| 1 Java 21 | feature/java21-migration | ✅ hecho | `.\mvnw.cmd -B verify` (JDK 21.0.13) → `Tests run: 79, Failures: 0, Errors: 0, Skipped: 2` · `BUILD SUCCESS` (52 s) · checkstyle: `0 Checkstyle violations` | `java.version=21`, records (`Vets`), `getFirst()` (SequencedCollection), hilos virtuales, checks de CI y Gradle a 21, JAXB muerto eliminado. Tag `legacy-java21` |
| 2 Esqueleto multi-módulo | feature/multimodule-skeleton | pendiente | | |
| 3 customers-service | feature/customers-service | pendiente | | |
| 4 vets-service | feature/vets-service | pendiente | | |
| 5 visits-service | feature/visits-service | pendiente | | |
| 6 api-gateway | feature/api-gateway | pendiente | | |
| 7 Paridad, calidad, Docker, CI | feature/parity-tests-and-docker | pendiente | | |
| 8 Documentación | feature/documentation | pendiente | | |
| 9 Release y publicación | release/1.0.0 | pendiente | | |

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

- Fase 9: crear repo público + `git push` requiere autenticación previa de `gh`.
