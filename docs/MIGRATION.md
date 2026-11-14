# Migración y decisiones técnicas

Este documento registra **qué** se cambió respecto al monolito original, **por qué**, y las
**versiones** usadas con su fuente de verificación (META_PROMPT §6).

## 1. Versiones verificadas

| Componente | Versión | Fuente consultada | Notas |
|---|---|---|---|
| Java | **21** (Temurin 21.0.x) | `java -version` local + [Adoptium](https://adoptium.net/temurin/releases/?version=21) | `maven.compiler.release=21` vía `java.version` del parent |
| Spring Boot | **4.1.0** | `pom.xml` del repo (parent) | Se mantiene la versión del repo: no hay incompatibilidad |
| Spring Cloud | **2025.1.4** (train *Oakwood*) | [spring.io/projects/spring-cloud](https://spring.io/projects/spring-cloud) → tabla «Release train Spring Boot compatibility» | *2025.1.x* soporta Boot 4.0.x **y 4.1.x** desde 2025.1.2. Verificado también en el POM del BOM (`spring-cloud-gateway.version=5.0.4`). **No hace falta el plan B** |
| Spring Cloud Gateway | **5.0.4** (`spring-cloud-starter-gateway-server-webmvc`) | `spring-cloud-gateway-dependencies-5.0.4.pom` en Maven Central | Existe variante **MVC** (servlet), sin WebFlux |
| springdoc-openapi | **3.2.0** (`springdoc-openapi-starter-webmvc-ui`, `-webflux-ui`) | [maven-metadata.xml en Maven Central](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml) | La línea **3.x** es la de Boot 4 (depende de artefactos modulares `spring-boot-tomcat`, `spring-boot-actuator-autoconfigure`) |
| Checkstyle | **13.11.0** | [maven-metadata.xml](https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/maven-metadata.xml) | Se sube desde 12.3.1: el pin existía porque v13 exige JDK 21 (`Release_13.0.0`) y ahora el build es 21 |

> Comprobación hecha con `curl`/navegador contra Maven Central y la documentación oficial el
> **2026-11-14**. No se ha fijado ninguna coordenada «de memoria».

## 2. Migración a Java 21

### 2.1 Cambios de build

| Archivo | Antes | Después |
|---|---|---|
| `pom.xml` | `<java.version>17</java.version>` | `<java.version>21</java.version>` |
| `pom.xml` | `checkstyle.version=12.3.1` (pin por JDK) | `13.11.0` |
| `build.gradle` | toolchain `17` | toolchain `21` |
| `.github/workflows/maven-build.yml` | JDK 17, `distribution: adopt` | **JDK 21**, `temurin`, trigger también en `develop` |
| `.github/workflows/gradle-build.yml` | JDK 17 | JDK 21 |
| `.devcontainer/devcontainer.json` | ya usaba `21-oracle` | sin cambios (coherente) |

### 2.2 Uso real de features de Java 21

Se aplican **donde aportan**, evitando reescribir lógica solo por lucir features:

| Feature | Dónde | Por qué aporta |
|---|---|---|
| **Records** | `vet/Vets.java` → `record Vets(List<Vet> vetList)` | El envoltorio de `/vets` era una clase mutable con lista perezosa; ahora es inmutable y Jackson lo serializa con el nombre del componente (`vetList`), sin cambiar el contrato JSON |
| **SequencedCollection** (`List#getFirst`) | `ClinicServiceTests` (`owner.getPets().getFirst()`, `vet.getSpecialties().getFirst()`) | Elimina índices mágicos `get(0)` en los tests |
| **Virtual threads** | `application.properties`: `spring.threads.virtual.enabled=true` | Tomcat y los ejecutores pasan a hilos virtuales: mejora de escalabilidad sin cambiar código |
| **Pattern matching for `instanceof`** | Ya presente en `PostgresIntegrationTests` (`source instanceof EnumerablePropertySource enumerable`) | Se conserva como ejemplo del estilo; no se fuerza en producción |
| **`String#strip`** | `OwnerController.processFindForm` (ya existía) | Se mantiene: es la forma correcta de *trim* Unicode |

**Lo que se decidió NO hacer** (y por qué):

- `switch` con patrones: no hay ninguna jerarquía sellada ni rama de tipos en el monolito; añadirlo
  sería código decorativo.
- Reescribir controladores con `record`s de formulario: se hará en la fase de microservicios, con
  DTOs reales, en lugar de duplicar el trabajo aquí.

### 2.3 Limpieza asociada

- Se elimina `jakarta.xml.bind:jakarta.xml.bind-api` y las anotaciones `@XmlRootElement`/`@XmlElement`.
  Motivo: **no había implementación JAXB ni `jackson-dataformat-xml`**, así que el XML de `/vets`
  nunca funcionó (ver BR-VET-02b en `BUSINESS_RULES.md`). Mantener anotaciones muertas induce a
  error; el soporte XML, si se recupera, se hará con `jackson-dataformat-xml` (opción de recorte #1).

## 3. Decisiones de comportamiento (ADR resumidos)

Los ADRs completos, con contexto/consecuencias, están en `docs/ARCHITECTURE.md`.

| ADR | Decisión | Motivo |
|---|---|---|
| ADR-001 | Microservicios por *bounded context* en monorepo Maven multi-módulo | Un `./mvnw verify` para el evaluador y servicios desplegables por separado |
| ADR-002 | Gateway con **Spring Cloud Gateway 5.0.4 variante MVC** | El train 2025.1.4 es compatible con Boot 4.1 → **no** hace falta el plan B; la variante MVC evita WebFlux y mantiene un solo stack |
| ADR-003 | Base de datos **por servicio** (H2 en memoria; perfil Postgres opcional) | Sin entidades compartidas; arranque sin infraestructura |
| ADR-004 | Paginación REST: `page`/`size` validados (`size ≤ 100`); página fuera de rango → **200 con la página 1** y metadatos | Un 302/400 por un parámetro de paginación es hostil para clientes; se documenta el cambio |
| ADR-005 | Errores uniformes con `ProblemDetail` (RFC 9457); «no existe» → **404**, duplicado → **409**, dependencia caída → **503** | El monolito devolvía 500 para recursos ausentes |
| ADR-006 | Sin *service discovery*; URLs por configuración/entorno | Menos piezas móviles para una prueba de 1 día |

## 4. Cambios de comportamiento respecto al original

| # | Original | Nuevo | Justificación |
|---|---|---|---|
| 1 | `GET /owners?page=0` → `302` a `page=1` | `200` con página 1 + metadatos | ADR-004 |
| 2 | Owner/pet/visit inexistente → `500` | `404` con `ProblemDetail` | ADR-005 |
| 3 | Nombre de pet duplicado → error de vista | `409` con `ProblemDetail` | Contrato REST explícito |
| 4 | Formularios HTML y redirecciones | JSON + Swagger UI + `.http` | Backend-only (fuera de alcance la UI) |
| 5 | `/vets` «JSON y XML» | JSON; XML opcional | BR-VET-02b: XML nunca funcionó |
| 6 | Búsqueda case-insensitive dependiente de H2 | `LOWER()`/`IgnoreCase` explícito en el repositorio | Igual comportamiento en H2/Postgres/MySQL |
| 7 | Listado sin orden estable | Orden fijo `lastName, firstName, id` | La paginación debe ser determinista (BR-OWN-14) |

## 5. Cómo verificar la migración de esta fase

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot'   # o el JDK 21 que uses
java -version        # debe reportar 21
.\mvnw.cmd -B verify # BUILD SUCCESS, mismo nº de tests que la línea base (79) o más
```
