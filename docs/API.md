# API REST

Todo se expone a través del gateway (**http://localhost:8080**) con el prefijo `/api/<servicio>`.
Cada servicio también publica OpenAPI y Swagger UI en su propio puerto, útil para depurar.

| Recurso | URL |
|---|---|
| Swagger UI agregado | http://localhost:8080/swagger-ui.html |
| OpenAPI de customers | http://localhost:8080/api/customers/v3/api-docs |
| OpenAPI de vets | http://localhost:8080/api/vets/v3/api-docs |
| OpenAPI de visits | http://localhost:8080/api/visits/v3/api-docs |
| OpenAPI del gateway | http://localhost:8080/v3/api-docs |
| Colección de pruebas | [`docs/http/*.http`](http/) |

Puertos directos (sin gateway): customers `:8081`, visits `:8082`, vets `:8083`.

## Modelo de errores (RFC 9457)

Todos los errores son `application/problem+json`:

```json
{
  "type": "about:blank#validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request body contains invalid fields",
  "instance": "/owners",
  "errors": [
    { "field": "telephone", "message": "Telephone must be a 10-digit number" }
  ]
}
```

| Situación | `status` | `type` |
|---|---|---|
| Recurso inexistente | `404` | `about:blank#not-found` |
| Cuerpo o parámetros inválidos | `400` | `about:blank#validation-failed` |
| Regla de negocio incumplida (fecha futura, tipo desconocido, tamaño de página) | `400` | `about:blank#bad-request` |
| Duplicado (nombre de mascota) | `409` | `about:blank#conflict` |
| Dependencia caída o timeout | `503` | `about:blank#service-unavailable` |

## Paginación (ADR-004)

- `page` es **1-based**, por defecto `1`.
- `size` por defecto `5`, máximo `100`; fuera de rango → `400`.
- `page < 1` o `page > totalPages` → se devuelve la **página 1** (nunca 302 ni 404).
- La respuesta incluye metadatos:

```json
{
  "content": [ ... ],
  "page": 1,
  "size": 5,
  "totalElements": 10,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

## customers-service

| Método | Ruta (gateway) | Códigos | Reglas |
|---|---|---|---|
| GET | `/api/customers/owners?lastName=&page=&size=` | 200, 400 | BR-OWN-06/07/08/14 |
| GET | `/api/customers/owners/{ownerId}` | 200, 404 | BR-OWN-09 |
| POST | `/api/customers/owners` | 201 + `Location`, 400 | BR-OWN-01..05, 11 |
| PUT | `/api/customers/owners/{ownerId}` | 200, 400, 404 | BR-OWN-10, 13 |
| GET | `/api/customers/petTypes` | 200 | BR-PET-08 |
| GET | `/api/customers/owners/{ownerId}/pets/{petId}` | 200, 404 | BR-PET-07 |
| POST | `/api/customers/owners/{ownerId}/pets` | 201 + `Location`, 400, 404, 409 | BR-PET-01..04, 10 |
| PUT | `/api/customers/owners/{ownerId}/pets/{petId}` | 200, 400, 404, 409 | BR-PET-02, 03, 05, 06 |

### Ejemplos

```bash
# Buscar por prefijo de apellido (case-insensitive)
curl "http://localhost:8080/api/customers/owners?lastName=fran&page=1&size=5"

# Crear un owner
curl -i -X POST http://localhost:8080/api/customers/owners \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","address":"12 Analytical St.","city":"London","telephone":"6000000001"}'

# Añadir una mascota
curl -i -X POST http://localhost:8080/api/customers/owners/1/pets \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada cat","birthDate":"2020-02-02","typeId":1}'
```

Respuesta de `GET /owners/1`:

```json
{
  "id": 1,
  "firstName": "George",
  "lastName": "Franklin",
  "address": "110 W. Liberty St.",
  "city": "Madison",
  "telephone": "6085551023",
  "pets": [
    { "id": 1, "name": "Leo", "birthDate": "2010-09-07", "type": { "id": 1, "name": "cat" } }
  ]
}
```

## vets-service

| Método | Ruta (gateway) | Códigos | Reglas |
|---|---|---|---|
| GET | `/api/vets?page=&size=` | 200, 400 | BR-VET-01 |
| GET | `/api/vets/all` | 200 | BR-VET-02/03 |

```bash
curl "http://localhost:8080/api/vets?page=1&size=5"
curl "http://localhost:8080/api/vets/all"      # { "vetList": [ ... ] }
```

```json
{
  "vetList": [
    { "id": 3, "firstName": "Linda", "lastName": "Douglas",
      "specialties": [ { "id": 3, "name": "dentistry" }, { "id": 2, "name": "surgery" } ],
      "nrOfSpecialties": 2 }
  ]
}
```

## visits-service

| Método | Ruta (gateway) | Códigos | Reglas |
|---|---|---|---|
| GET | `/api/visits/owners/{ownerId}/pets/{petId}/visits` | 200, 404, 503 | BR-PET-09, BR-VIS-03 |
| POST | `/api/visits/owners/{ownerId}/pets/{petId}/visits` | 201 + `Location`, 400, 404, 503 | BR-VIS-01..03 |
| GET | `/api/visits/pets/visits?petId=7,8` | 200 | composición |

```bash
# Reservar (la fecha debe ser estrictamente futura)
curl -i -X POST http://localhost:8080/api/visits/owners/6/pets/7/visits \
  -H 'Content-Type: application/json' \
  -d '{"date":"2027-01-15","description":"annual checkup"}'
```

> El puerto 8082 habla con customers-service (`CUSTOMERS_SERVICE_URL`). Si está caído, la API
> responde **503** en lugar de 500.

## api-gateway

| Método | Ruta | Códigos |
|---|---|---|
| GET | `/api/owners/{ownerId}/details` | 200, 404, 503 |
| GET | `/api/system/boom` | 500 (demostración, BR-SYS-01) |

```bash
curl http://localhost:8080/api/owners/6/details
```

```json
{
  "id": 6,
  "firstName": "Jean",
  "lastName": "Coleman",
  "address": "105 N. Lake St.",
  "city": "Monona",
  "telephone": "6085552654",
  "pets": [
    {
      "id": 7,
      "name": "Samantha",
      "birthDate": "2012-09-04",
      "type": { "id": 1, "name": "cat" },
      "visits": [
        { "id": 1, "date": "2013-01-01", "description": "rabies shot" },
        { "id": 4, "date": "2013-01-04", "description": "spayed" }
      ]
    },
    { "id": 8, "name": "Max", "birthDate": "2012-09-04", "type": { "id": 1, "name": "cat" }, "visits": [ ... ] }
  ]
}
```

## Lista completa de reglas y su test

- Inventario funcional: [`docs/agent/BUSINESS_RULES.md`](agent/BUSINESS_RULES.md)
- Trazabilidad regla → test y cobertura: [`docs/TESTING.md`](TESTING.md)
