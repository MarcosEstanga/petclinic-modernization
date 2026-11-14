# PetClinic Modernization — instrucciones para agentes

Punto de entrada único. Lo leen Cursor, Claude Code, OpenCode y cualquier
agente que soporte `AGENTS.md`. **No dupliques contenido aquí**: enruta a la
fuente. Claude Code / Claude Agent también leen `CLAUDE.md`, que solo apunta aquí.

## Qué harness usar

| Trabajo | Dónde | Por qué |
|---|---|---|
| Explorar monolito, docs, cambios repetitivos | **OpenCode + Go** (Flash) | Barato; alto volumen |
| Arquitectura, migraciones, debug oscuro, fases difíciles | **Claude Code** o OpenCode **Claude Agent** | Razonamiento; no quemarlo en lectura |
| Planificar antes de implementar | OpenCode agente `plan` o Claude Code | Piensa primero; ejecuta después |
| Revisión de paridad / OpenAPI / tests | OpenCode agente `review` o Claude Code | Calidad antes de merge |

Regla de cuota: Flash/Go primero → si falla, Claude.
Nunca dos agentes escribiendo el mismo árbol sin ramas/worktrees distintos.

## Misión

Prueba técnica de 1 día: modernizar `spring-petclinic` (monolito Spring Boot + Thymeleaf)
hacia **Java 21** y una **arquitectura de microservicios** con **REST + OpenAPI**, **tests**,
**documentación**, **uso documentado de IA** y **GitFlow**. Solo se evalúa el backend.

## Orden de lectura (obligatorio antes de tocar código)

| # | Archivo | Para qué |
|---|---|---|
| 1 | `docs/agent/META_PROMPT.md` | Rol, reglas duras, forma de trabajar |
| 2 | `docs/agent/PLAN.md` | Fases, ramas GitFlow, commits y criterio de "hecho" por fase |
| 3 | `docs/agent/BUSINESS_RULES.md` | Inventario de reglas/validaciones del monolito que hay que preservar |
| 4 | `docs/agent/ARCHITECTURE_TARGET.md` | Arquitectura objetivo, contratos REST, decisiones |
| 5 | `docs/agent/CHECKLIST.md` | Definition of Done final mapeada a los criterios de evaluación |
| 6 | `docs/agent/PROGRESS.md` | Estado vivo: lo actualizas tú al cerrar cada fase |

## Reglas de convivencia

- Trabaja **siempre en una rama `feature/*` desde `develop`**; nunca commits directos a `main`.
- Nunca dos agentes escribiendo el mismo árbol sin ramas/worktrees distintos.
- Antes de decir "hecho": `./mvnw -q verify` en verde (o el comando de la fase) y salida pegada en `PROGRESS.md`.
- Registra cada uso relevante de IA en `docs/AI_USAGE.md` (ver formato en el META_PROMPT).
- No commitees secretos, `.env`, `target/`, logs ni dumps.
- Entorno: Windows 11. PowerShell usa `;` (no `&&`). Hay Git Bash disponible. `./mvnw` (no hay `mvn` global).
- Acciones externas (crear repo en GitHub, `git push`, instalar JDK) → **pide confirmación al usuario** la primera vez.
