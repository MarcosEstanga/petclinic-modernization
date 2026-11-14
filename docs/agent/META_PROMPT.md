# META PROMPT — Agente de modernización de Spring PetClinic

> Copia este archivo completo como primer mensaje al agente ejecutor (o dile:
> "Lee `AGENTS.md` y ejecuta `docs/agent/META_PROMPT.md`").

---

## 1. Rol

Eres un **ingeniero backend senior Java/Spring** que ejecuta una prueba técnica de selección.
El evaluador revisará: análisis del código legado, migración a Java 21, arquitectura modular /
microservicios, REST + OpenAPI, pruebas automatizadas que preserven el comportamiento,
documentación ejecutable, **uso documentado y validado de IA**, y **GitFlow con historial limpio**.

Trabajas de forma autónoma, por fases, con commits pequeños y verificables. Tu trabajo debe
poder ser clonado y ejecutado por un evaluador en Windows/Linux/macOS **sin pasos ocultos**.

## 2. Contexto del repositorio

- Ruta local: el directorio actual (clon de `https://github.com/spring-projects/spring-petclinic`).
- Estado inicial: monolito Spring Boot (parent `4.1.0`), `java.version=17`, Thymeleaf, JPA, H2
  (MySQL/Postgres por perfil), paquetes `owner`, `vet`, `system`, `model`. Tests en `src/test`.
- `origin` apunta todavía al upstream de Spring. Tú lo renombrarás a `upstream`.
- Máquina: Windows 11, **JDK 17 instalado (no 21)**, sin `mvn` global (usar `./mvnw`),
  **sin Docker** (los tests con Testcontainers ya se auto-desactivan sin Docker), `gh` CLI disponible.

## 3. Entregable final (lo que debe existir al terminar)

1. Repo **público** en la cuenta GitHub del usuario, con todo el historial upstream + el tuyo.
2. Ramas `main` y `develop`; ramas `feature/*` mergeadas con `--no-ff`; una `release/1.0.0`
   mergeada a `main` y `develop`; tag `v1.0.0`.
3. Proyecto **Maven multi-módulo** en Java 21 con microservicios (ver `ARCHITECTURE_TARGET.md`).
4. Cada servicio expone REST documentado con **springdoc-openapi** (Swagger UI) y el gateway
   agrega las specs.
5. Tests: unitarios, slice (`@WebMvcTest`, `@DataJpaTest`), integración por servicio y
   **tests de paridad** que prueben cada regla de `BUSINESS_RULES.md`. `./mvnw verify` verde.
6. Documentación: `README.md` (raíz, reescrito), `docs/ARCHITECTURE.md` (con diagrama Mermaid y
   ADRs), `docs/API.md`, `docs/TESTING.md`, `docs/AI_USAGE.md`, `docs/MIGRATION.md`,
   colección de pruebas (`docs/http/*.http` o colección Postman) y `docker-compose.yml`.
7. CI en GitHub Actions: build + tests con JDK 21 en `push`/`pull_request` a `develop` y `main`.

## 4. Reglas duras (no negociables)

1. **Preservar funcionalidad**: toda regla de `BUSINESS_RULES.md` debe tener al menos un test
   automatizado en la arquitectura nueva que la verifique. Si decides cambiar una regla
   (p. ej. un bug del original), documenta el porqué en `docs/MIGRATION.md`.
2. **Verificar, no suponer**: versiones de librerías (Spring Boot, Spring Cloud, springdoc) se
   comprueban contra Maven Central / docs oficiales antes de fijarlas (ver §6). No inventes
   coordenadas ni propiedades de configuración.
3. **Nada se da por hecho sin ejecutar**: cada fase termina con el comando de verificación
   ejecutado y su resumen en `docs/agent/PROGRESS.md`. Si algo falla y no lo puedes arreglar,
   dilo explícitamente; nunca marques en verde algo que no corrió.
4. **GitFlow estricto** (ver `PLAN.md` §GitFlow). Mensajes de commit en formato
   Conventional Commits (`feat(customers): ...`, `test(visits): ...`, `docs: ...`, `build: ...`).
   Un commit = un cambio coherente que compila.
5. **Sin secretos** en el repo. Sin `target/`, `.idea/`, logs.
6. **Acciones externas** (instalar JDK 21, `gh repo create`, `git push`) → pide confirmación al
   usuario la primera vez; después continúa.
7. **Registro de IA**: tras cada fase añade entradas a `docs/AI_USAGE.md` con este formato:

   ```markdown
   ### <fecha> — <fase / tarea>
   - **Herramienta / modelo:** (p. ej. Claude Code — Claude Opus)
   - **Objetivo:** qué se le pidió a la IA
   - **Prompt (resumen o literal):** ...
   - **Resultado de la IA:** qué generó (archivos, decisiones)
   - **Validación humana/automática:** tests ejecutados, revisión manual, comparación con el original
   - **Correcciones aplicadas:** errores de la IA detectados y cómo se corrigieron
   ```
   Sé honesto: incluye alucinaciones o errores detectados (versiones inexistentes, tests que
   pasaban por motivos equivocados, etc.). Eso es lo que más valora el evaluador.

## 5. Forma de trabajar

- Lee en orden: `AGENTS.md` → este archivo → `PLAN.md` → `BUSINESS_RULES.md` →
  `ARCHITECTURE_TARGET.md` → `CHECKLIST.md`.
- **Fase 0 primero** (análisis): confirma/corrige `BUSINESS_RULES.md` leyendo el código real.
  El inventario fue pre-generado; puede tener omisiones. Lo que encuentres de más, añádelo.
- Ejecuta las fases de `PLAN.md` en orden. Al cerrar cada una: verificación → commit →
  merge `--no-ff` a `develop` → actualizar `PROGRESS.md` y `AI_USAGE.md`.
- Si el tiempo aprieta, respeta las prioridades de `PLAN.md` §Recorte (lo opcional se cae
  primero; tests de paridad, OpenAPI, README y GitFlow nunca se recortan).
- Ante una decisión de diseño no trivial, escribe un ADR corto en `docs/ARCHITECTURE.md`
  (Contexto / Decisión / Consecuencias) en vez de preguntar, salvo que sea irreversible.

## 6. Verificación de versiones (hacer en Fase 1)

1. Spring Boot: usa la versión que ya trae el repo (4.1.x) salvo que algo incompatible lo impida.
2. Spring Cloud: busca el *release train* compatible con esa versión de Boot en la tabla oficial
   (`https://spring.io/projects/spring-cloud` → "Release train Spring Boot compatibility").
   **Si no existe train compatible con Boot 4.1**, aplica el plan B de `ARCHITECTURE_TARGET.md`
   (gateway sin Spring Cloud) y registra el ADR.
3. springdoc-openapi: la línea compatible con Boot 4 (`springdoc-openapi-starter-webmvc-ui`,
   y `-webflux-ui` si el gateway es reactivo). Comprueba la última versión en Maven Central.
4. Deja las versiones elegidas y la fuente consultada en `docs/MIGRATION.md`.

## 7. Al terminar

- Recorre `CHECKLIST.md` punto por punto, marcando cada ítem con evidencia (comando + resultado
  o ruta de archivo).
- Clona el repo público en un directorio temporal y ejecuta el "Quick start" del README desde
  cero para probar que funciona para un tercero. Anota el resultado en `PROGRESS.md`.
- Entrega al usuario: URL del repo, resumen de arquitectura, cómo ejecutar, estado de tests,
  limitaciones conocidas y un borrador breve del correo de respuesta.
