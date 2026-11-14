# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado
[SemVer](https://semver.org/lang/es/).

## [1.0.0] — 2026-11-14

Primera versión de la modernización del monolito `spring-petclinic` (commit `500158f`) a Java 21 y
microservicios REST documentados.

### Añadido

- **Java 21**: `java.version=21`, hilos virtuales, `record`s y colecciones secuenciadas.
- **Arquitectura multi-módulo**: `libs/common-web`, `services/customers-service` (:8081),
  `services/vets-service` (:8083), `services/visits-service` (:8082), `api-gateway` (:8080) y
  `tests/parity-tests`.
- **REST + OpenAPI** en los tres servicios con springdoc 3.2.0 (`/v3/api-docs`, Swagger UI) y
  Swagger agregado en el gateway con una pestaña por servicio.
- **Errores uniformes** con `ProblemDetail` (RFC 9457): 400, 404, 409 y 503.
- **Gateway** con Spring Cloud Gateway 5.0.4 (variante MVC): rutas `/api/customers`, `/api/vets`,
  `/api/visits` y composición `GET /api/owners/{id}/details`.
- **Validación inter-servicio**: `visits-service` valida owner/pet contra `customers-service` con
  `RestClient` + `@HttpExchange`, timeouts y traducción 404→404 / caída→503.
- **Caché Caffeine** para el listado de vets (`vets`), con documento de paridad BR-VET-04.
- **117 tests**: unitarios, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, cliente HTTP con
  `MockRestServiceServer`, gateway con stub HTTP y suite de paridad regla → test.
- **JaCoCo** por módulo (93–97 % de líneas) y `docs/TESTING.md` con la trazabilidad completa.
- **Docker**: `Dockerfile` multi-stage y `docker-compose.yml` con healthchecks.
- **CI** en GitHub Actions con JDK 21 (`./mvnw -B verify`).
- **Documentación**: `README.md`, `docs/ANALYSIS.md`, `docs/ARCHITECTURE.md` (ADR-001…006),
  `docs/API.md`, `docs/TESTING.md`, `docs/MIGRATION.md`, `docs/AI_USAGE.md` y `docs/http/*.http`.

### Cambiado

- Paginación REST: página fuera de rango → `200` con la página 1 y metadatos (antes `302`).
- Búsqueda sin resultados → `200` con página vacía (antes error de vista).
- Recurso inexistente → `404` con `ProblemDetail` (antes `500`).
- Duplicado de nombre de mascota → `409` (antes error de vista).
- Orden estable en los listados (`lastName, firstName, id`).

### Eliminado

- Vistas Thymeleaf, SCSS, WebJars e i18n de UI (fuera de alcance; conservadas en el tag
  `legacy-java21`).
- Build Gradle, manifiestos de k8s, workflow de despliegue a clúster, check DCO y Checkstyle.
- Dependencia muerta `jakarta.xml.bind-api` y las anotaciones JAXB (el XML de `/vets` nunca funcionó).

### Conservado

- Historial completo del repositorio upstream y el monolito migrado en el tag anotado
  `legacy-java21`.

[1.0.0]: https://github.com/MarcosEstanga/petclinic-modernization/releases/tag/v1.0.0
