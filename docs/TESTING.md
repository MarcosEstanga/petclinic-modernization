# Testing

## Cómo ejecutar

```bash
# Todo (build + tests + paridad + JaCoCo)
./mvnw verify

# Un módulo
./mvnw -pl services/customers-service -am test

# Una clase o un test
./mvnw -pl services/customers-service test -Dtest=OwnerControllerTests
./mvnw -pl services/customers-service test -Dtest='OwnerControllerTests#BR_OWN_05*'

# Informe de cobertura (HTML por módulo)
#   services/customers-service/target/site/jacoco/index.html
#   services/vets-service/target/site/jacoco/index.html
#   services/visits-service/target/site/jacoco/index.html
#   api-gateway/target/site/jacoco/index.html
```

Requisitos: **JDK 21** (el `maven-enforcer-plugin` falla con menos) y conexión a Maven Central la
primera vez. **No hace falta Docker**: H2 en memoria y *stubs* en proceso sustituyen a los servicios.

## Pirámide de tests por módulo

| Módulo | Unitarios | `@WebMvcTest` | `@DataJpaTest` | `@SpringBootTest` | Otros | Total |
|---|---|---|---|---|---|---|
| customers-service | `OwnerServiceTests` (9), `PetServiceTests` (10) | `OwnerControllerTests` (13), `PetControllerTests` (8), `PetTypeControllerTests` (1) | `CustomersRepositoryTests` (8) | `CustomersServiceIntegrationTests` (6), `CustomersServiceApplicationTests` (1) | — | **56** |
| vets-service | `VetServiceTests` (5) | `VetControllerTests` (3) | `VetsRepositoryTests` (3) | `VetsServiceIntegrationTests` (3), `VetsServiceApplicationTests` (1) | `VetCachingTests` (1) | **16** |
| visits-service | `VisitServiceTests` (8), `CustomersGatewayTests` (4, `MockRestServiceServer`) | `VisitControllerTests` (7), `PetVisitsControllerTests` (1) | `VisitRepositoryTests` (4) | `VisitsServiceIntegrationTests` (5), `VisitsServiceApplicationTests` (1) | — | **30** |
| api-gateway | `OwnerDetailsComposerTests` (5) | — | — | `GatewayIntegrationTests` (7, stub HTTP del JDK), `ApiGatewayApplicationTests` (1) | — | **13** |
| parity-tests | — | — | — | — | `ParityTraceabilityTests` (2) | **2** |
| **Total** | | | | | | **117** |

`./mvnw verify` en verde: **117 tests, 0 fallos** (2026-11-14, JDK 21.0.13, Windows 11).

### Qué prueba cada nivel

- **Unitarios**: reglas de negocio puras y casos de uso con repositorios mockeados (paginación,
  duplicados, fechas, traducción de errores).
- **`@WebMvcTest`**: contrato HTTP (201 + `Location`, 400/404/409/503 y forma de `ProblemDetail`),
  Bean Validation y el `@RestControllerAdvice`.
- **`@DataJpaTest`**: esquema real, constraints (`unique_owner_pet_name`), búsquedas
  case-insensitive, orden (`@OrderBy`) y datos semilla.
- **`@SpringBootTest`**: servicio completo por HTTP real (puerto aleatorio, H2 y scripts SQL).
- **Cliente inter-servicio**: `MockRestServiceServer` sobre la interfaz `@HttpExchange`
  (timeout, 404, 500).
- **Paridad**: guardián estático que falla si una regla de `BUSINESS_RULES.md` no tiene test con su
  ID (o no está en la lista justificada de excepciones).

## Cobertura (JaCoCo)

Generada con `./mvnw verify` (objetivo ≥ 80 % en dominio y servicios):

| Módulo | Líneas cubiertas / totales | Cobertura de líneas | Cobertura de ramas |
|---|---|---|---|
| customers-service | 154 / 165 | **93,3 %** | 75,0 % |
| vets-service | 57 / 59 | **96,6 %** | 87,5 % |
| visits-service | 85 / 88 | **96,6 %** | 75,0 % |
| api-gateway | 71 / 73 | **97,3 %** | 100 % |

Lo no cubierto son ramas defensivas (por ejemplo códigos remotos distintos de 404/5xx) y clases de
configuración sin lógica. `libs/common-web` no tiene tests propios porque su comportamiento se
verifica a través de los `@WebMvcTest` de los servicios; `tests/parity-tests` es una suite estática.

## Trazabilidad regla → test

Cada fila de [`docs/agent/BUSINESS_RULES.md`](agent/BUSINESS_RULES.md) apunta a su test. Resumen:

| Regla | Test principal |
|---|---|
| BR-OWN-01 | `OwnerControllerTests#BR_OWN_01_firstNameIsMandatory` |
| BR-OWN-02 | `OwnerControllerTests#BR_OWN_02_lastNameIsLimitedTo30Characters` · `CustomersRepositoryTests#BR_OWN_02_and_BR_OWN_06_searchByLastNamePrefixIsCaseInsensitive` |
| BR-OWN-03 | `OwnerControllerTests#BR_OWN_03_addressIsLimitedTo255Characters` |
| BR-OWN-04 | `OwnerControllerTests#BR_OWN_04_cityIsLimitedTo80Characters` |
| BR-OWN-05 | `OwnerControllerTests#BR_OWN_05_telephoneMustHaveExactly10Digits` |
| BR-OWN-06 | `OwnerServiceTests#BR_OWN_06_lastNameIsTrimmedAndUsedAsAPrefix` · `#BR_OWN_06_blankLastNameReturnsEveryOwner` |
| BR-OWN-07 | `OwnerServiceTests#BR_OWN_07_pageBeyondTheLastPageIsServedAsFirstPage` · `CustomersServiceIntegrationTests#outOfRangePageIsServedAsFirstPageWithoutRedirecting` |
| BR-OWN-08 | `OwnerServiceTests#BR_OWN_08_noMatchesReturnsAnEmptyPageInsteadOfAnError` |
| BR-OWN-09 | `OwnerServiceTests#BR_OWN_09_unknownOwnerIsANotFound` · `CustomersServiceIntegrationTests#unknownOwnerIsAProblemDetail404` |
| BR-OWN-10 | `OwnerControllerTests#BR_OWN_10_and_BR_OWN_11_identifierInTheBodyIsIgnored` |
| BR-OWN-11 | `OwnerControllerTests#BR_OWN_10_and_BR_OWN_11_identifierInTheBodyIsIgnored` |
| BR-OWN-12 | `OwnerServiceTests#BR_OWN_12_pageBelowOneIsClampedInsteadOfBreakingTheQuery` |
| BR-OWN-13 | `OwnerControllerTests#BR_OWN_13_updateUsesTheIdentifierFromTheUrl` |
| BR-OWN-14 | `OwnerServiceTests#BR_OWN_14_paginationUsesAStableSort` · `CustomersRepositoryTests#BR_OWN_14_pagesAreStableWithTheDocumentedSort` |
| BR-PET-01 | `PetControllerTests#BR_PET_01_nameIsMandatoryAndLimitedTo30Characters` |
| BR-PET-02 | `PetServiceTests#BR_PET_02_typeIsRequiredWhenCreating` |
| BR-PET-03 | `PetControllerTests#BR_PET_03_*` · `PetServiceTests#BR_PET_03_birthDateInTheFutureIsRejected` |
| BR-PET-04 | `PetServiceTests#BR_PET_04_duplicateNameForTheSameOwnerIsAConflict` · `CustomersRepositoryTests#BR_PET_04_*` · `CustomersServiceIntegrationTests#duplicatePetNameEndsUpAs409` |
| BR-PET-05 | `PetServiceTests#BR_PET_05_reusingItsOwnNameIsAllowed` · `#BR_PET_05_renamingToAnotherPetsNameIsAConflict` |
| BR-PET-06 | `PetServiceTests#BR_PET_06_databaseUniquenessViolationBecomesAConflict` · `#BR_PET_06_otherIntegrityViolationsArePropagated` |
| BR-PET-07 | `PetServiceTests#BR_PET_07_petOfAnotherOwnerIsANotFound` · `CustomersRepositoryTests#BR_PET_07_aPetIsOnlyReachableThroughItsOwner` |
| BR-PET-08 | `PetTypeControllerTests#BR_PET_08_typesAreReturnedInTheServiceOrder` · `CustomersRepositoryTests#BR_PET_08_petTypesAreOrderedByName` |
| BR-PET-09 | `CustomersRepositoryTests#BR_PET_09_petsOfAnOwnerAreOrderedByName` · `VisitRepositoryTests#BR_PET_09_visitsAreOrderedByDate` |
| BR-PET-10 | `PetServiceTests#BR_PET_10_unknownPetTypeIsAClientError` |
| BR-PET-11 | *(no se conserva; ver MIGRATION §4)* |
| BR-PET-12 | `VisitServiceTests#BR_PET_12_aPetThatDoesNotBelongToTheOwnerCannotBeUsedForAVisit` |
| BR-VIS-01 | `VisitControllerTests#BR_VIS_01_descriptionIsMandatoryAndLimitedTo255Characters` |
| BR-VIS-02 | `VisitServiceTests#BR_VIS_02_aDateFromTodayIsRejected` · `#BR_VIS_02_aDateInThePastIsRejected` · `VisitsServiceIntegrationTests#aPastDateIsRejected` |
| BR-VIS-03 | `CustomersGatewayTests#BR_VIS_03_*` · `VisitControllerTests#BR_VIS_03_*` · `VisitsServiceIntegrationTests#BR_VIS_03_*` |
| BR-VIS-04 | `VisitServiceTests#BR_VIS_04_aNewVisitDefaultsToTomorrow` |
| BR-VIS-05 | `VisitServiceTests#BR_VIS_03_and_BR_VIS_05_nothingIsStoredWhenThePetDoesNotBelongToTheOwner` |
| BR-VET-01 | `VetServiceTests#BR_VET_01_*` · `VetControllerTests#BR_VET_01_paginatedListIncludesMetadata` |
| BR-VET-02 | `VetControllerTests#BR_VET_02_fullListIsWrappedInVetList` · `VetsServiceIntegrationTests#allVetsAreReturnedInTheLegacyShape` |
| BR-VET-03 | `VetServiceTests#BR_VET_03_specialtiesAreSortedByNameAndCounted` · `VetsRepositoryTests#BR_VET_03_specialtiesComeOrderedAndCounted` |
| BR-VET-04 | `VetCachingTests#BR_VET_04_theVetListIsServedFromTheCacheOnTheSecondCall` |
| BR-SYS-01 | `GatewayIntegrationTests#BR_SYS_01_boomEndpointFailsOnPurpose` |
| BR-SYS-02 | `OwnerControllerTests#BR_SYS_02_telephoneMessageComesFromTheMessageBundle` |
| BR-SYS-03 | `CustomersRepositoryTests#BR_SYS_03_seedDataIsTheLegacyOne` · `VetsRepositoryTests#BR_SYS_03_*` · `VisitRepositoryTests#BR_SYS_03_*` |

Reglas sin test automatizado (justificadas en el propio inventario y en `MIGRATION.md`):
BR-VET-02b (XML inexistente, código eliminado), BR-PET-11 (sustituida por la constraint),
BR-SYS-04 (perfiles de BD documentados) y BR-SYS-05 (exposición de Actuator).

## Suite de paridad (`tests/parity-tests`)

`ParityTraceabilityTests` lee `docs/agent/BUSINESS_RULES.md`, extrae los IDs y comprueba que cada uno
aparece, como `BR_OWN_05`, en el código de tests de los módulos. Si alguien añade una regla y se
olvida del test, **el build falla**. Las excepciones están declaradas y se valida que sigan siendo
reglas reales.

## Limitaciones conocidas

- **Sin Docker en la máquina de desarrollo**: los tests de Testcontainers del monolito no se
  portaron; los sustitutos son H2 + stubs en proceso. La CI de GitHub tampoco arranca contenedores.
- **Sin tests de contrato con Pact**: la comunicación visits → customers se prueba con
  `MockRestServiceServer`, que valida la forma del contrato pero no un consumidor real.
- **Sin pruebas de carga** (el `src/test/jmeter` del monolito quedó en el tag `legacy-java21`).
- La UI (Thymeleaf) no se porta, así que no hay tests de vistas.
