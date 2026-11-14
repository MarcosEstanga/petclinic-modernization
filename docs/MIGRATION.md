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

### 2.4 Herramientas y artefactos retirados

| Artefacto | Acción | Motivo |
|---|---|---|
| `build.gradle`, `settings.gradle`, `gradlew*`, `gradle/` | eliminados | Doble build mantenía dos fuentes de verdad; la CI solo usa Maven |
| `k8s/`, `.github/workflows/deploy-and-test-cluster.yml` | eliminados | El despliegue a clúster no forma parte del alcance y la imagen no se puede probar aquí |
| `.github/workflows/gradle-build.yml` | eliminado | Sin Gradle |
| `.github/workflows/maven-build.yml` | sustituido por `ci.yml` | Un único workflow con JDK 21, `verify` y artefacto JaCoCo |
| `.github/dco.yml` | eliminado | Exigía *sign-off* en cada commit (política del upstream, no de este repo) |
| `src/checkstyle/**` | eliminado | El reactor multi-módulo no usa Checkstyle; se mantiene `spring-javaformat` |
| `src/main/resources/templates`, `static`, `scss` | eliminados (tag `legacy-java21`) | UI fuera de alcance |
| `src/test/jmeter` | eliminado (tag `legacy-java21`) | Pruebas de carga fuera de alcance |

### 2.5 Estructura final

```
pom.xml (parent, Java 21) → libs/common-web, services/{customers,vets,visits}-service, api-gateway, tests/parity-tests
```

Cada servicio sigue la misma estructura de paquetes (`api` → `application` → `domain`, con
`infrastructure` para repositorios/clientes/configuración), lo que hace la migración del monolito
predecible: cada controlador MVC se convirtió en un controlador REST con DTOs `record`, cada regla de
validación en un test con su ID y cada vista en una respuesta JSON.

Los ADRs completos, con contexto/consecuencias, están en `docs/ARCHITECTURE.md`.

## 3. Decisiones de comportamiento (ADR resumidos)

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

## 5. Cómo verificar la migración

```bash
# JDK 21
export JAVA_HOME=/ruta/al/jdk-21        # Windows: $env:JAVA_HOME='C:\...\jdk-21'
java -version                           # 21.x

./mvnw -B verify                        # 117 tests + paridad + JaCoCo
docker compose up --build               # opcional, requiere Docker
curl http://localhost:8080/api/owners/6/details
```

En la máquina de desarrollo la verificación se hizo con **Temurin 21.0.13**:

```
Tests run: 117, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(el resumen por módulo y la cobertura están en `docs/TESTING.md`).

## 6. Lo que se migró y lo que no

| Original | Estado | Dónde |
|---|---|---|
| Entidades, repositorios y reglas de owners/pets | migrado a REST | `customers-service` |
| Visitas (fecha futura, pertenencia al owner) | migrado + validación remota | `visits-service` |
| Vets y especialidades (solo lectura, caché) | migrado | `vets-service` |
| `/vets` JSON con `vetList` | migrado | `vets-service` `/vets/all` |
| Vistas Thymeleaf, SCSS, WebJars, i18n de UI | **no** migrado (fuera de alcance) | tag `legacy-java21` |
| Página `ownerDetails` | migrado como composición REST | `api-gateway` `/api/owners/{id}/details` |
| `/oups` | migrado como `/api/system/boom` | `api-gateway` |
| Perfiles MySQL/PostgreSQL | documentados, no implementados por servicio | `MIGRATION.md` §1 |
