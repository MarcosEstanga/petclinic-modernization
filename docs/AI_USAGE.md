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
