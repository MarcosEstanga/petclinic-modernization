# Inventario de reglas de negocio y validaciones (monolito original)

> Pre-generado leyendo `src/main` (commit `500158f`). **Validado línea a línea en la Fase 0**
> contra el código real (ver `docs/ANALYSIS.md`). Las reglas añadidas en la validación van
> marcadas con **(nueva)** y las correcciones con **(corregida)**.
> La columna **Test** se rellena en las fases 3–7 con `Clase#metodo` del servicio que la cubre.

## Owners — `owner/Owner.java`, `model/Person.java`, `OwnerController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-OWN-01 | `firstName` obligatorio (`@NotBlank`), máx. 30 (`@Size(max=30)`, columna `VARCHAR(30)`) | `Person` | |
| BR-OWN-02 | `lastName` obligatorio, máx. 30; en H2 la columna es `VARCHAR_IGNORECASE(30)`, por eso la búsqueda es case-insensitive | `Person`, `schema.sql` | |
| BR-OWN-03 | `address` obligatorio, máx. 255 | `Owner` | |
| BR-OWN-04 | `city` obligatorio, máx. 80 | `Owner` | |
| BR-OWN-05 | `telephone` obligatorio y exactamente 10 dígitos (`@Pattern("\\d{10}")`, mensaje `telephone.invalid`) | `Owner` | |
| BR-OWN-06 | Búsqueda por **prefijo** de apellido (`findByLastNameStartingWith`); se aplica `strip()`; apellido nulo o solo espacios = todos | `OwnerController.processFindForm` | |
| BR-OWN-07 | Paginación de búsqueda: tamaño **5**; `page < 1` o `page > max(totalPages,1)` → se redirige a página 1 conservando `lastName` | `OwnerController` | |
| BR-OWN-08 | Sin resultados → error de validación `notFound` sobre `lastName`; exactamente 1 resultado → ir directo al detalle de ese owner | `OwnerController` | |
| BR-OWN-09 | Owner inexistente → `IllegalArgumentException` (en MVC acaba en 500; en REST debe ser **404**) | `findOwner`/`showOwner` | |
| BR-OWN-10 | En update, el `id` del cuerpo debe coincidir con el de la URL; si no → error de validación `mismatch` y redirect a la edición | `processUpdateOwnerForm` | |
| BR-OWN-11 | El cliente no puede fijar `id` ni `*.id` (`@InitBinder#setDisallowedFields`); en REST, ignorar/rechazar `id` en create | `OwnerController`, `PetController`, `VisitController` | |
| BR-OWN-12 | Un `page < 1` **no debe romper** el paginado: se satura a 1 antes de `PageRequest.of` (commits `0d064b0`, `b33136f`). El original lanzaba `IllegalArgumentException` | `findPaginatedForOwnersLastName`, `VetController.findPaginated` | |
| BR-OWN-13 | **(nueva)** Tras validar un update, el `id` se **fuerza** al de la URL (`owner.setId(ownerId)`); el id del cuerpo nunca persiste | `processUpdateOwnerForm` | |
| BR-OWN-14 | **(nueva)** El listado no define `Sort`: el orden entre páginas no está garantizado por contrato. En la nueva arquitectura se fija un orden estable (`lastName, firstName, id`) | `OwnerRepository` | |

## Pets — `owner/Pet.java`, `PetValidator.java`, `PetController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-PET-01 | `name` obligatorio (`@NotBlank` en `NamedEntity` + `PetValidator`) y máx. 30 (validador; columna `VARCHAR_IGNORECASE(30)`) | `PetValidator`, `schema.sql` | |
| BR-PET-02 | `type` obligatorio **solo al crear** (`pet.isNew()`); en update el validador lo permite omitir | `PetValidator` | |
| BR-PET-03 | `birthDate` obligatoria y **no futura** (hoy está permitido; el validador exige no nula y el controlador rechaza `isAfter(now)`) | `PetValidator`, `PetController` | |
| BR-PET-04 | Nombre de pet **único por owner, case-insensitive** (validación en app + `unique_owner_pet_name`) → en REST **409** | `PetController`, `schema.sql` | |
| BR-PET-05 | En update, renombrar a un nombre ya usado por **otro** pet del mismo owner → error; mantener el mismo nombre → OK (se compara por `id`) | `processUpdateForm` | |
| BR-PET-06 | Violación de integridad por nombre duplicado en carrera (`unique_owner_pet_name`) se traduce a error de validación; **otras** violaciones se propagan | `isDuplicatePetNameViolation`, `PetClinicConcurrencyTests` | |
| BR-PET-07 | Pet debe pertenecer al owner de la URL (`owner.getPet(petId)`); si no, no existe para esa ruta | `PetController.findPet`, `Owner.getPet` | |
| BR-PET-08 | Listado de tipos de mascota ordenado por nombre (`ORDER BY ptype.name`) | `PetTypeRepository.findPetTypes` | |
| BR-PET-09 | Pets de un owner ordenados por nombre (`@OrderBy("name")`); visitas de un pet ordenadas por fecha ascendente (`@OrderBy("date ASC")`) | `Owner`, `Pet` | |
| BR-PET-10 | **(nueva)** El `type` se resuelve por **nombre exacto** (`PetTypeFormatter.parse`); un tipo desconocido es un error de binding (`400`), nunca un tipo inventado | `PetTypeFormatter` | |
| BR-PET-11 | **(nueva)** `Owner.addPet` ignora `null` y no duplica: descarta el mismo objeto o un pet con el mismo `id` ya persistido | `Owner.addPet` | |
| BR-PET-12 | **(nueva)** Un `petId` que no pertenece al owner en el formulario de visita produce `IllegalArgumentException` (en REST **404**) | `VisitController.loadPetWithVisit` | |

## Visits — `owner/Visit.java`, `VisitController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-VIS-01 | `description` obligatoria (`@NotBlank`), máx. 255 | `Visit` | |
| BR-VIS-02 | `date` debe ser **estrictamente futura** (`> hoy`; la fecha mínima del formulario es mañana) | `VisitController` | |
| BR-VIS-03 | Owner y pet deben existir y el pet pertenecer al owner; si no → `IllegalArgumentException` (REST **404**) | `loadPetWithVisit` | |
| BR-VIS-04 | Fecha por defecto de una visita nueva = mañana (`LocalDate.now().plusDays(1)`), fijada en el constructor | `Visit` | |
| BR-VIS-05 | **(nueva)** `Owner.addVisit(petId, visit)` valida pet y visita no nulos y que el pet exista (`Assert`), impidiendo visitas huérfanas | `Owner.addVisit` | |

## Vets — `vet/*`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-VET-01 | Listado paginado de 5; página fuera de rango (`<1` o `> max(totalPages,1)`) → página 1 | `VetController.showVetList` | |
| BR-VET-02 | Listado completo en `/vets` envuelto en objeto `Vets` (`vetList`) | `VetController.showResourcesVetList` | |
| BR-VET-02b | **(corregida)** El original **solo devuelve JSON**: no hay `jackson-dataformat-xml` ni implementación JAXB (`jakarta.xml.bind-api` es solo API), así que las anotaciones `@XmlRootElement/@XmlElement` no tienen efecto. El XML de `/vets` que menciona la documentación upstream **no funciona**. XML queda opcional en la nueva arquitectura (recorte #1) | `pom.xml`, `Vets`, `Vet` | |
| BR-VET-03 | Especialidades de un vet ordenadas por nombre y `nrOfSpecialties` = tamaño del conjunto | `Vet` | |
| BR-VET-04 | Listado de vets cacheado en la caché `vets` (`@Cacheable` + JCache/Caffeine) | `CacheConfiguration`, `VetRepository` | |

## Sistema

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-SYS-01 | Endpoint que provoca un error intencionado (`/oups`, HTTP 500 con el mensaje de la excepción) para demostrar el manejo de errores | `CrashController` | (se porta como `/api/system/boom` y se documenta) |
| BR-SYS-02 | i18n de mensajes de validación (`messages_*.properties`, 11 idiomas, incluido `telephone.invalid`) y sincronía de claves entre idiomas | `resources/messages`, `I18nPropertiesSyncTest` | |
| BR-SYS-03 | Datos semilla idénticos al original: 10 owners, 13 pets, 6 types, 6 vets, 3 specialties, 4 visits | `db/h2/data.sql` | |
| BR-SYS-04 | Perfiles MySQL/PostgreSQL soportados además del H2 por defecto | `application-*.properties`, `db/*/schema.sql` | |
| BR-SYS-05 | **(nueva)** Actuator expone **todos** los endpoints (`management.endpoints.web.exposure.include=*`); en el monolito es deliberado (demo), en producción hay que restringirlo | `application.properties` | |

## Reglas del original que **no** se conservan (y por qué)

| Regla | Decisión | Motivo |
|---|---|---|
| Redirección HTTP 302 al paginar fuera de rango | **Cambia** a 200 con la página 1 y metadatos de paginación | Un cliente REST no debe recibir un 302 por un parámetro; se documenta en `docs/MIGRATION.md` (ADR-004) |
| `IllegalArgumentException` → HTTP 500 para "no existe" | **Cambia** a 404 | El 500 no distingue error de servidor de recurso ausente (ADR-005) |
| Mensajes de error de vista (`typeMismatch.birthDate`, `is already in use`) | **Cambia** a `ProblemDetail` con `errors[].field/message` | Contrato uniforme RFC 9457 |
| Vistas Thymeleaf y navegación HTML | **No se portan** | Fuera de alcance declarado (solo backend); la UI es Swagger + `.http` |
| XML en `/vets` | **Opcional** (primera opción de recorte) | No funcionaba en el original (BR-VET-02b) |

## Cómo leer esta tabla

- Un ID = al menos un test automatizado cuyo nombre contiene el ID
  (p. ej. `BR_OWN_05_telephoneMustHaveExactly10Digits`).
- Si una regla cambia, el test verifica el comportamiento **nuevo** y `docs/MIGRATION.md` explica
  el cambio con su ADR.
- La tabla de trazabilidad final regla → test está en `docs/TESTING.md`.
