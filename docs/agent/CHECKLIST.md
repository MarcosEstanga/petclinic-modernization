# Definition of Done — mapeada a los criterios de evaluación

Marca cada ítem con `[x]` y **evidencia** (comando + resultado resumido, o ruta).

## 1. Análisis del código existente
- [ ] `docs/ANALYSIS.md`: dominio (ER Mermaid), endpoints originales, dependencias, deuda técnica, bounded contexts
- [ ] `docs/agent/BUSINESS_RULES.md` validado contra el código y completo (columna Test rellena)
- [ ] Línea base de tests del monolito registrada (nº tests, resultado)

## 2. Migración a Java 21+
- [ ] `<java.version>21</java.version>` (o `maven.compiler.release=21`) en el POM padre
- [ ] CI usa JDK 21; devcontainer/`.gitpod.yml`/`build.gradle` coherentes o eliminados y documentado
- [ ] Uso real de features de Java 21 (records, pattern matching, sequenced collections…) listado en `docs/MIGRATION.md`
- [ ] Versiones de librerías verificadas y con fuente en `docs/MIGRATION.md`

## 3. Arquitectura modular / microservicios
- [ ] Multi-módulo Maven: customers, vets, visits, gateway (+ common opcional)
- [ ] BD por servicio; sin entidades compartidas
- [ ] Comunicación visits → customers con manejo de errores (404/503) y timeouts
- [ ] Capas por servicio (api / application / domain / infrastructure)
- [ ] `docs/ARCHITECTURE.md` con diagrama y ADRs (al menos: microservicios vs modulith, gateway, BD por servicio, paginación REST, errores)

## 4. REST + OpenAPI
- [ ] springdoc en cada servicio; `/v3/api-docs` y `/swagger-ui.html` responden
- [ ] Swagger agregado en el gateway
- [ ] DTOs con `@Schema` (descripciones, ejemplos), `@Operation` con códigos de respuesta
- [ ] Errores con `ProblemDetail`; códigos HTTP correctos (201+Location, 400, 404, 409, 503)
- [ ] `docs/API.md` con tabla de endpoints y ejemplos; `docs/http/*.http` (o Postman) con flujo E2E

## 5. Pruebas automatizadas y preservación funcional
- [ ] Un test por regla de `BUSINESS_RULES.md`, nombrado con el ID
- [ ] Unit + `@WebMvcTest` + `@DataJpaTest` + `@SpringBootTest` por servicio
- [ ] Test del cliente HTTP inter-servicio (WireMock / MockRestServiceServer)
- [ ] `./mvnw verify` verde desde clon limpio (pegar resumen: módulos, nº tests)
- [ ] Cobertura JaCoCo reportada en `docs/TESTING.md`
- [ ] Tabla de trazabilidad regla → test en `docs/TESTING.md`

## 6. Documentación técnica (README raíz)
- [ ] Requisitos (JDK 21, Docker opcional), Quick start en ≤5 comandos
- [ ] Compilar, ejecutar cada servicio (local, `./mvnw -pl ... spring-boot:run`), ejecutar con Docker Compose
- [ ] Puertos, URLs de Swagger, perfiles, variables de entorno
- [ ] Cómo correr tests y ver cobertura
- [ ] Estructura del repo y enlaces a `docs/*`
- [ ] Estrategia de ramas y cómo contribuir
- [ ] Limitaciones conocidas y trabajo futuro

## 7. Uso de IA
- [ ] `docs/AI_USAGE.md`: herramientas, entradas por fase, prompts, validación, **errores de la IA detectados y corregidos**
- [ ] Sección "Cómo se validó lo generado por IA" (tests, revisión, comparación con el original)
- [ ] Los archivos `AGENTS.md`/`docs/agent/*` se conservan como evidencia del meta-prompting

## 8. Git / GitFlow
- [ ] Historial upstream preservado
- [ ] Ramas `main`, `develop`, `feature/*`, `release/1.0.0` visibles en el remoto
- [ ] Merges `--no-ff`; tag `v1.0.0`; Conventional Commits
- [ ] Repo **público**; CI en verde en GitHub
- [ ] Prueba de humo desde clon limpio realizada
