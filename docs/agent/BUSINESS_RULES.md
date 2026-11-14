# Inventario de reglas de negocio y validaciones (monolito original)

> Pre-generado leyendo `src/main` (commit `500158f`). **Validado línea a línea en la Fase 0**
> contra el código real (ver `docs/ANALYSIS.md`). Las reglas añadidas en la validación van
> marcadas con **(nueva)** y las correcciones con **(corregida)**.
> La columna **Test** apunta al test de la arquitectura nueva que la verifica (se completa a
> medida que avanza cada servicio).

## Owners — `owner/Owner.java`, `model/Person.java`, `OwnerController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-OWN-01 | `firstName` obligatorio (`@NotBlank`), máx. 30 (`@Size(max=30)`, columna `VARCHAR(30)`) | `Person` | `OwnerControllerTests#BR_OWN_01_firstNameIsMandatory` |
| BR-OWN-02 | `lastName` obligatorio, máx. 30; en H2 la columna es `VARCHAR_IGNORECASE(30)`, por eso la búsqueda es case-insensitive | `Person`, `schema.sql` | `OwnerControllerTests#BR_OWN_02_lastNameIsLimitedTo30Characters`, `CustomersRepositoryTests#BR_OWN_02_and_BR_OWN_06_searchByLastNamePrefixIsCaseInsensitive` |
| BR-OWN-03 | `address` obligatorio, máx. 255 | `Owner` | `OwnerControllerTests#BR_OWN_03_addressIsLimitedTo255Characters` |
| BR-OWN-04 | `city` obligatorio, máx. 80 | `Owner` | `OwnerControllerTests#BR_OWN_04_cityIsLimitedTo80Characters` |
| BR-OWN-05 | `telephone` obligatorio y exactamente 10 dígitos (`@Pattern("\\d{10}")`, mensaje `telephone.invalid`) | `Owner` | `OwnerControllerTests#BR_OWN_05_telephoneMustHaveExactly10Digits` |
| BR-OWN-06 | Búsqueda por **prefijo** de apellido (`findByLastNameStartingWith`); se aplica `strip()`; apellido nulo o solo espacios = todos | `OwnerController.processFindForm` | `OwnerServiceTests#BR_OWN_06_*`, `CustomersRepositoryTests#BR_OWN_02_and_BR_OWN_06_*` |
| BR-OWN-07 | Paginación de búsqueda: tamaño **5**; `page < 1` o `page > max(totalPages,1)` → se sirve la **página 1** (ADR-004) | `OwnerController` | `OwnerServiceTests#BR_OWN_07_pageBeyondTheLastPageIsServedAsFirstPage`, `CustomersServiceIntegrationTests#outOfRangePageIsServedAsFirstPageWithoutRedirecting` |
| BR-OWN-08 | Sin resultados → **200 con página vacía** (antes: error de vista `notFound`); exactamente 1 resultado → se devuelve en `content` (antes: redirect al detalle) | `OwnerController` | `OwnerServiceTests#BR_OWN_08_noMatchesReturnsAnEmptyPageInsteadOfAnError` |
| BR-OWN-09 | Owner inexistente → **404** (antes `IllegalArgumentException` → 500) | `findOwner`/`showOwner` | `OwnerServiceTests#BR_OWN_09_unknownOwnerIsANotFound`, `OwnerControllerTests#BR_OWN_09_*`, `CustomersServiceIntegrationTests#unknownOwnerIsAProblemDetail404` |
| BR-OWN-10 | El `id` del cuerpo nunca manda: en update manda el de la URL; si el cuerpo trae `id`, se ignora | `processUpdateOwnerForm` | `OwnerControllerTests#BR_OWN_10_and_BR_OWN_11_identifierInTheBodyIsIgnored`, `#updateUsesTheIdentifierFromTheUrl` |
| BR-OWN-11 | El cliente no puede fijar `id` ni `*.id` | `@InitBinder` | `OwnerControllerTests#BR_OWN_10_and_BR_OWN_11_*` |
| BR-OWN-12 | Un `page < 1` **no debe romper** el paginado: se satura a 1 antes de `PageRequest.of` (commits `0d064b0`, `b33136f`) | `OwnerController`, `VetController` | `OwnerServiceTests#BR_OWN_12_pageBelowOneIsClampedInsteadOfBreakingTheQuery` |
| BR-OWN-13 | **(nueva)** En update el `id` se **fuerza** al de la URL | `processUpdateOwnerForm` | `OwnerControllerTests#updateUsesTheIdentifierFromTheUrl` |
| BR-OWN-14 | **(nueva)** El listado no define `Sort` en el original; aquí se fija un orden estable (`lastName, firstName, id`) | `OwnerRepository` | `OwnerServiceTests#BR_OWN_14_paginationUsesAStableSort`, `CustomersRepositoryTests#BR_OWN_14_pagesAreStableWithTheDocumentedSort` |

## Pets — `owner/Pet.java`, `PetValidator.java`, `PetController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-PET-01 | `name` obligatorio y máx. 30 | `PetValidator`, `schema.sql` | `PetControllerTests#BR_PET_01_nameIsMandatoryAndLimitedTo30Characters` |
| BR-PET-02 | `type` obligatorio **solo al crear**; en update se puede omitir y se conserva el actual | `PetValidator` | `PetServiceTests#BR_PET_02_typeIsRequiredWhenCreating`, `#BR_PET_05_reusingItsOwnNameIsAllowed` |
| BR-PET-03 | `birthDate` obligatoria y **no futura** (hoy permitido) | `PetValidator`, `PetController` | `PetControllerTests#BR_PET_03_birthDateIsMandatory`, `#BR_PET_03_birthDateInTheFutureIsRejectedByTheService`, `PetServiceTests#BR_PET_03_birthDateInTheFutureIsRejected` |
| BR-PET-04 | Nombre **único por owner, case-insensitive** → **409** | `PetController`, `schema.sql` | `PetServiceTests#BR_PET_04_duplicateNameForTheSameOwnerIsAConflict`, `CustomersRepositoryTests#BR_PET_04_*`, `CustomersServiceIntegrationTests#duplicatePetNameEndsUpAs409` |
| BR-PET-05 | En update, renombrar a un nombre usado por **otro** pet del owner → error; mantener el mismo nombre → OK | `processUpdateForm` | `PetServiceTests#BR_PET_05_reusingItsOwnNameIsAllowed`, `#BR_PET_05_renamingToAnotherPetsNameIsAConflict` |
| BR-PET-06 | Violación de `unique_owner_pet_name` (carrera) → conflicto; **otras** violaciones se propagan | `isDuplicatePetNameViolation` | `PetServiceTests#BR_PET_06_databaseUniquenessViolationBecomesAConflict`, `#BR_PET_06_otherIntegrityViolationsArePropagated` |
| BR-PET-07 | Pet debe pertenecer al owner de la URL | `PetController.findPet`, `Owner.getPet` | `PetServiceTests#BR_PET_07_petOfAnotherOwnerIsANotFound`, `PetControllerTests#BR_PET_07_unknownOwnerReturns404`, `CustomersRepositoryTests#BR_PET_07_aPetIsOnlyReachableThroughItsOwner` |
| BR-PET-08 | Listado de tipos de mascota ordenado por nombre | `PetTypeRepository.findPetTypes` | `PetTypeControllerTests#BR_PET_08_typesAreReturnedInTheServiceOrder`, `CustomersRepositoryTests#BR_PET_08_petTypesAreOrderedByName` |
| BR-PET-09 | Pets de un owner ordenados por nombre; visitas ordenadas por fecha | `@OrderBy` | `CustomersRepositoryTests#BR_PET_09_petsOfAnOwnerAreOrderedByName` (visitas en `visits-service`) |
| BR-PET-10 | **(nueva)** El `type` debe existir; un tipo desconocido es `400`, nunca un tipo inventado | `PetTypeFormatter` | `PetServiceTests#BR_PET_10_unknownPetTypeIsAClientError` |
| BR-PET-11 | **(nueva)** `Owner.addPet` del original ignoraba duplicados en memoria | `Owner.addPet` | **No se conserva**: el agregado es bidireccional y los duplicados los rechaza el servicio + la constraint (BR-PET-04/06). Ver `MIGRATION.md` §4 |
| BR-PET-12 | **(nueva)** Un `petId` que no pertenece al owner en el formulario de visita → `404` | `VisitController.loadPetWithVisit` | Se cubre en `visits-service` (Fase 5) |

## Visits — `owner/Visit.java`, `VisitController.java`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-VIS-01 | `description` obligatoria (`@NotBlank`), máx. 255 | `Visit` | Fase 5 |
| BR-VIS-02 | `date` debe ser **estrictamente futura** (`> hoy`; mínima del formulario = mañana) | `VisitController` | Fase 5 |
| BR-VIS-03 | Owner y pet deben existir y el pet pertenecer al owner; si no → **404** | `loadPetWithVisit` | Fase 5 |
| BR-VIS-04 | Fecha por defecto de una visita nueva = mañana | `Visit` (constructor) | Fase 5 |
| BR-VIS-05 | `Owner.addVisit(petId, visit)` valida pet y visita no nulos y que el pet exista | `Owner.addVisit` | Fase 5 |

## Vets — `vet/*`

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-VET-01 | Listado paginado de 5; página fuera de rango → página 1 | `VetController.showVetList` | `VetServiceTests#BR_VET_01_aPageBeyondTheEndIsServedAsFirstPage`, `#BR_VET_01_pageBelowOneIsClampedAndSortedStably`, `VetControllerTests#BR_VET_01_paginatedListIncludesMetadata` |
| BR-VET-02 | Listado completo en `/vets/all` envuelto en objeto `Vets` (`vetList`) | `VetController.showResourcesVetList` | `VetServiceTests#BR_VET_02_fullListIsWrappedInAVetsObject`, `VetControllerTests#BR_VET_02_fullListIsWrappedInVetList`, `VetsServiceIntegrationTests#allVetsAreReturnedInTheLegacyShape` |
| BR-VET-02b | **(corregida)** El original **solo devuelve JSON**: no hay `jackson-dataformat-xml` ni implementación JAXB, así que las anotaciones `@XmlRootElement/@XmlElement` no tienen efecto; el XML documentado **no funciona**. Se eliminó el código muerto (Fase 1); XML queda opcional (recorte #1) | `pom.xml`, `Vets`, `Vet` | — |
| BR-VET-03 | Especialidades ordenadas por nombre y `nrOfSpecialties` = tamaño | `Vet` | `VetServiceTests#BR_VET_03_specialtiesAreSortedByNameAndCounted`, `VetsRepositoryTests#BR_VET_03_specialtiesComeOrderedAndCounted` |
| BR-VET-04 | Listado de vets cacheado en la caché `vets` | `CacheConfiguration`, `VetRepository` | `VetCachingTests#BR_VET_04_theVetListIsServedFromTheCacheOnTheSecondCall` |

## Sistema

| ID | Regla | Origen | Test |
|---|---|---|---|
| BR-SYS-01 | Endpoint que provoca un error intencionado (HTTP 500) para demostrar el manejo de errores | `CrashController` | Fase 6 (`/api/system/boom`) |
| BR-SYS-02 | i18n de mensajes de validación (incl. `telephone.invalid`) | `resources/messages` | `OwnerControllerTests#BR_OWN_05_*` (mensaje traducido desde `ValidationMessages.properties`) |
| BR-SYS-03 | Datos semilla idénticos al original: 10 owners, 13 pets, 6 types, 6 vets, 3 specialties, 4 visits | `db/h2/data.sql` | `CustomersRepositoryTests#BR_SYS_03_seedDataIsTheLegacyOne` (owners/pets/types) + Fase 4 (vets) + Fase 5 (visits) |
| BR-SYS-04 | Perfiles MySQL/PostgreSQL soportados | `application-*.properties`, `db/*/schema.sql` | Fase 8 (documentado; H2 por defecto) |
| BR-SYS-05 | **(nueva)** Actuator exponía **todos** los endpoints en el monolito; aquí se limita a `health,info,metrics` | `application.properties` | Verificado en `application.properties` de cada servicio |

## Reglas del original que **no** se conservan (y por qué)

| Regla | Decisión | Motivo |
|---|---|---|
| Redirección HTTP 302 al paginar fuera de rango | **Cambia** a 200 con la página servida y metadatos | Un cliente REST no debe recibir un 302 por un parámetro (ADR-004) |
| Sin resultados de búsqueda → error de campo | **Cambia** a 200 con página vacía | La búsqueda es una consulta, no un recurso (ADR-004) |
| `IllegalArgumentException` → HTTP 500 para "no existe" | **Cambia** a 404 | El 500 no distingue error de servidor de recurso ausente (ADR-005) |
| Mensajes de error de vista (`typeMismatch.birthDate`, `is already in use`) | **Cambia** a `ProblemDetail` con `errors[].field/message` | Contrato uniforme RFC 9457 |
| `Owner.addPet` ignora duplicados en memoria | **Cambia**: lo garantizan el servicio y la constraint | El agregado es bidireccional y la unicidad se comprueba en BD (BR-PET-04/06) |
| Vistas Thymeleaf y navegación HTML | **No se portan** | Fuera de alcance declarado (solo backend) |
| XML en `/vets` | **Opcional** (primera opción de recorte) | No funcionaba en el original (BR-VET-02b) |

## Cómo leer esta tabla

- Un ID = al menos un test automatizado cuyo nombre contiene el ID
  (p. ej. `BR_OWN_05_telephoneMustHaveExactly10Digits`).
- Si una regla cambia, el test verifica el comportamiento **nuevo** y `docs/MIGRATION.md` explica
  el cambio con su ADR.
- La tabla de trazabilidad final regla → test está en `docs/TESTING.md`.
