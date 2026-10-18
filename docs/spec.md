# Especificación — Star Wars Catalog API

Es la especificación con la que se desarrolló la API: el contrato y los escenarios verificables (dado, cuando, entonces). Cada escenario con ID `E` tiene al menos un test automatizado; los que no se pueden automatizar están en [No unit-testeables](#no-unit-testeables).

## Qué y por qué
API REST en Java 21 y Spring Boot que expone el catálogo de People, Films, Starships y Vehicles de swapi.tech, paginado, filtrable por id y/o nombre, y protegido con login y roles ([`CHALLENGE.md`](../CHALLENGE.md), requisitos 1 a 4). Además tiene que estar documentada (uso y código), cubierta por tests unitarios y de integración, y desplegada en un servidor gratuito para que el revisor la pruebe (requisitos 5 a 7).

Decisiones:
- **Proxy con caché:** cada consulta se traduce a SWAPI; la paginación se delega a SWAPI donde la soporta y se hace en memoria donde no (films y búsquedas por nombre). La caché protege el límite de 100 requests por ventana.
- **Usuarios predefinidos** por configuración, sin registro ni base de datos.
- **Roles `USER` y `ADMIN`:** `USER` lee el catálogo; `ADMIN` además puede vaciar la caché y ver métricas.
- **Atributos numéricos tipados:** los textos numéricos de SWAPI se exponen como números, y los valores desconocidos como `null`. Las relaciones se exponen como ids.

### Contrato

| Método y ruta | Auth | Respuesta |
|---|---|---|
| `POST /api/v1/auth/login` `{username, password}` | Pública | `200 {accessToken, tokenType: "Bearer", expiresIn}` |
| `GET /api/v1/{people\|films\|starships\|vehicles}?page&size&id&name` | `USER` o `ADMIN` | `200 {content[], page, size, totalElements, totalPages}` |
| `GET /api/v1/{people\|films\|starships\|vehicles}/{id}` | `USER` o `ADMIN` | `200` entidad |
| `DELETE /api/v1/admin/cache` | `ADMIN` | `204` |
| `GET /actuator/health` | Pública | Salud |
| `GET /actuator/metrics/**` | `ADMIN` | Métricas (caché, Resilience4j, HTTP) |
| `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | Pública | Swagger UI y contrato OpenAPI |

- Paginación: `page` base 1 (default 1, sin máximo), `size` entre 1 y 100 (default 10). Toda respuesta paginada, incluidas las filtradas por id, informa el `page` y `size` pedidos.
- Errores: Problem Details (RFC 9457) con `title`, `status`, `detail` e `instance` (`type` se omite cuando es `about:blank`).
- El filtro `name` se llama igual en los cuatro recursos; en films filtra por título.
- El orden de los resultados es el orden en que los devuelve SWAPI; la API no reordena.
- Las rutas públicas responden igual con o sin header `Authorization`, aunque el token sea inválido.
- Actuator no expone ningún endpoint fuera de `health` y `metrics`.

### Tipos de los atributos

| Recurso | Entero (`Long`) | Decimal (`Double`) | Otros tipos | Texto |
|---|---|---|---|---|
| people | `height` | `mass` | `homeworldId`; `filmIds`, `starshipIds`, `vehicleIds` (listas de ids) | `name`, `gender`, `birthYear`, `hairColor`, `skinColor`, `eyeColor` |
| films | — | — | `episodeId` (`Integer`), `releaseDate` (fecha); `characterIds`, `planetIds`, `starshipIds`, `vehicleIds`, `speciesIds` | `title`, `director`, `producer`, `openingCrawl` |
| starships | `costInCredits`, `crew`, `passengers`, `cargoCapacity`, `maxAtmospheringSpeed`, `mglt` | `length`, `hyperdriveRating` | `pilotIds`, `filmIds` | `name`, `model`, `manufacturer`, `starshipClass`, `consumables` |
| vehicles | `costInCredits`, `crew`, `passengers`, `cargoCapacity`, `maxAtmospheringSpeed` | `length` | `pilotIds`, `filmIds` | `name`, `model`, `manufacturer`, `vehicleClass`, `consumables` |

Todas las entidades exponen además `id` (texto, el `uid` de SWAPI).

## Fuera de alcance
- Registro de usuarios, refresh tokens, revocación de tokens y logout.
- Protección contra fuerza bruta en el login y rate limiting de la API propia (se documentan como "con más tiempo").
- Frontend: Swagger UI y la colección de Bruno cubren la prueba manual.
- Otros recursos de SWAPI (planets, species) y expandir relaciones a objetos completos: se exponen solo como ids.
- Ordenamiento configurable y filtros por otros campos.
- Persistencia propia de datos de SWAPI.
- Hacer el deploy en la cuenta de Render: el repo deja todo listo para que sea un paso manual corto.
- Tests de tiempos de respuesta del login (serían inestables; la mitigación está en el diseño: ver el ADR 0004).

## Escenarios

### Autenticación y autorización
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E1 | El usuario predefinido `user` con su contraseña | `POST /auth/login` con credenciales correctas | 200 con `accessToken` JWT firmado, `tokenType` `Bearer` y `expiresIn` igual a la vigencia configurada (default 3600) |
| E2 | Un usuario existente o inexistente | `POST /auth/login` con contraseña incorrecta o usuario desconocido | 401 Problem Details con el mismo `detail` en los dos casos (no revela si el usuario existe) |
| E3 | — | `POST /auth/login` sin `username`, sin `password` o con alguno en blanco | 400 Problem Details que nombra el campo inválido |
| E4 | — | `GET /people` sin header `Authorization` | 401 Problem Details |
| E5 | Un token vencido, con firma inválida o mal formado | `GET /people` con ese token | 401 Problem Details |
| E6 | Un token válido de `USER` | `GET /people` | 200 |
| E7 | Un token válido de `USER` | `DELETE /admin/cache` | 403 Problem Details |
| E8 | Un token válido de `ADMIN` y una consulta ya cacheada | `DELETE /admin/cache` y repetir la consulta | 204, y la consulta repetida vuelve a llamar a SWAPI |
| E9 | — | `GET /actuator/health`, `/swagger-ui/index.html`, `/v3/api-docs` y `/v3/api-docs/swagger-config` sin token | 200 en los cuatro |
| E42 | Un token vencido o mal formado | `POST /auth/login` con credenciales correctas, `GET /actuator/health` o `GET /v3/api-docs` con ese token en `Authorization` | Responden igual que sin token (200), no 401 |
| E43 | — | `GET /actuator/metrics` sin token / con token de `USER` / con token de `ADMIN` | 401 / 403 / 200 |
| E44 | — | `GET /actuator/info`, `/actuator/env` o `/actuator/beans`, con cualquier token | 404 (no expuestos) |

### Listado paginado
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E10 | SWAPI con 82 people | `GET /people?page=2&size=10` | Llama a SWAPI `GET /people?page=2&limit=10&expanded=true` una vez; responde `content` con 10 personas, `page` 2, `size` 10, `totalElements` 82, `totalPages` 9 |
| E11 | — | `GET /people` sin `page` ni `size` | Equivale a `page=1&size=10` |
| E12 | SWAPI con 82 people (9 páginas de 10) | `GET /people?page=10&size=10` | 200 con `content` vacío, `totalElements` 82 y `totalPages` 9 (no devuelve la última página que recorta SWAPI) |
| E13 | — | `page` < 1, `size` < 1, `size` > 100, o `page`/`size` no numéricos | 400 Problem Details que nombra el parámetro |
| E14 | SWAPI con 6 films | `GET /films?page=2&size=4` | Llama a SWAPI `GET /films` una vez; responde los films 5.º y 6.º en el orden de SWAPI, `totalElements` 6, `totalPages` 2 |
| E15 | SWAPI con starships y vehicles | `GET /starships` y `GET /vehicles` paginados | Mismo comportamiento que E10 sobre cada recurso |
| E45 | SWAPI con 6 films / 2 coincidencias para `sky` | `GET /films?page=3&size=4` / `GET /people?name=sky&page=3&size=1` | 200 con `content` vacío y los totales reales (`totalElements` 6, `totalPages` 2 / `totalElements` 2, `totalPages` 2) |
| E46 | SWAPI con 6 films | `GET /films?page=2147483647&size=100` y lo mismo con `?name=` | 200 con `content` vacío y los totales reales; nunca 500 por desborde |

### Filtrado
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E16 | SWAPI devuelve 2 coincidencias para `sky` | `GET /people?name=sky&page=1&size=1` | Llama a SWAPI `GET /people?name=sky`; responde 1 persona, `totalElements` 2, `totalPages` 2 |
| E17 | — | `GET /films?name=hope` | Llama a SWAPI `GET /films?title=hope` (nunca `?name=`) |
| E18 | SWAPI sin coincidencias | `GET /people?name=zzz` | 200 con `content` vacío, `totalElements` 0, `totalPages` 0 |
| E19 | `name` vacío o solo espacios | `GET /people?name=%20` | Se ignora el filtro: se comporta como E10/E11 |
| E20 | SWAPI tiene la persona 1 | `GET /people?id=1` | Llama a SWAPI `GET /people/1`; responde `content` con 1 elemento, `page` 1, `size` 10, `totalElements` 1, `totalPages` 1 |
| E21 | SWAPI responde 404 para el id 9999 | `GET /people?id=9999` | 200 con `content` vacío, `page` 1, `size` 10, `totalElements` 0, `totalPages` 0 (como filtro no es un error) |
| E22 | La persona 1 se llama `Luke Skywalker` | `GET /people?id=1&name=LUKE` / `GET /people?id=1&name=vader` | Página con Luke (coincidencia parcial sin distinguir mayúsculas) / página vacía con `totalElements` 0 |
| E23 | — | `id` no numérico o ≤ 0, en el query param o en la ruta | 400 Problem Details |
| E47 | SWAPI tiene la persona 1 | `GET /people?id=1&page=2` | 200 con `content` vacío, `page` 2, `totalElements` 1, `totalPages` 1 (el resultado por id pasa por la misma paginación que el resto) |

### Detalle
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E24 | SWAPI tiene la persona 1 | `GET /people/1` | 200 con la persona |
| E25 | SWAPI responde 404 para el id 9999 | `GET /people/9999` | 404 Problem Details |

### Mapeo de datos
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E26 | Valores en campos enteros `"172"`, `"843,342"`, `"1000000000000"` | Se mapean | `172`, `843342`, `1000000000000` (entero de 64 bits) |
| E27 | Valores en campos decimales `"36.8 "`, `"4.0"`, `"1,358"` | Se mapean | `36.8`, `4.0`, `1358.0` |
| E28 | Valores `"unknown"`, `"n/a"`, `"none"`, `""`, `"30-165"` en un campo numérico | Se mapean | `null` |
| E48 | Cualquier valor que no se puede interpretar como número en un campo numérico, o una fecha que no parsea en `releaseDate` | Se mapea | El campo queda en `null`; el resto de la entidad se mapea igual y el request nunca falla por eso |
| E29 | Relaciones `"https://www.swapi.tech/api/films/1"` y `homeworld` | Se mapean | Ids `"1"`; `homeworld` como `homeworldId`; listas vacías como `[]` |
| E30 | Un film con `release_date` `"1977-05-25"` y `episode_id` 4 | Se mapea | `releaseDate` fecha `1977-05-25`, `episodeId` 4 |
| E31 | La respuesta de SWAPI con `results` (listado) o `result` (búsqueda, detalle, films) | Se mapea | Ambos envoltorios producen las mismas entidades de dominio |

### Caché y resiliencia
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E32 | Una consulta ya resuelta dentro del TTL (default 1 hora) | Se repite la misma consulta | SWAPI recibe una sola llamada |
| E49 | SWAPI responde 404 para el id 9999 | `GET /people/9999` dos veces dentro del TTL | SWAPI recibe una sola llamada; las dos respuestas son 404 |
| E50 | La caché vacía y SWAPI respondiendo con demora | Varios requests idénticos concurrentes a `GET /people` | SWAPI recibe una sola llamada y todos los requests responden 200 con el mismo contenido |
| E33 | SWAPI responde 5xx de forma persistente | `GET /people` | 3 intentos en total con backoff; luego 502 Problem Details |
| E34 | SWAPI responde 5xx una vez y después 200 | `GET /people` | 200 (el reintento lo recupera) |
| E35 | SWAPI responde 404 o 429 | Cualquier consulta | No reintenta: una sola llamada a SWAPI |
| E36 | SWAPI no responde dentro del timeout de lectura (default 5 s) | `GET /people` | No reintenta (una sola llamada); 503 Problem Details con `Retry-After` igual al default configurado (30) |
| E51 | SWAPI rechaza la conexión de forma persistente | `GET /people` | 3 intentos en total con backoff; luego 503 Problem Details con `Retry-After` igual al default configurado (30) |
| E37 | SWAPI responde 429 con `x-ratelimit-reset` | `GET /people` | 503 Problem Details con `Retry-After` igual a los segundos hasta ese reset (mínimo 1); sin el header, el default configurado (30) |
| E38 | El circuit breaker abierto por fallas | `GET /people` | 503 Problem Details con `Retry-After` igual a la espera configurada del breaker en estado abierto (default 30), sin llamar a SWAPI |
| E52 | El circuit breaker cerrado y una consulta cacheada | Se repite la consulta muchas veces | Los hits de caché no se registran como llamadas en el breaker |

### Transversales
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E39 | Un request con header `X-Request-Id` válido / sin él | Cualquier endpoint | La respuesta devuelve el mismo `X-Request-Id` / uno generado (UUID); los logs de ese request lo incluyen |
| E53 | Un `X-Request-Id` de más de 64 caracteres o con caracteres fuera de `[A-Za-z0-9._-]` (incluidos CR y LF) | Cualquier endpoint | Se descarta y se usa un UUID generado, en la respuesta y en los logs |
| E40 | El código fuente | Corre el test de arquitectura | `domain` no depende de Spring, Jackson, `adapter` ni `application`; `application` no depende de Spring, Jackson ni `adapter`; ninguno de los dos depende de Jakarta, Resilience4j, Caffeine, `security` ni `config`; `adapter.in` y `adapter.out` no dependen entre sí; `adapter.out` no depende de `security` |
| E41 | El secreto JWT ausente o de menos de 32 bytes | Arranca la aplicación | Falla al arrancar con un mensaje que nombra la variable `JWT_SECRET` |

### Robustez
| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| E54 | La app corriendo en un contenedor real (no MockMvc) | Un 401 por token ausente, por token inválido o por login fallido | La respuesta incluye `WWW-Authenticate: Bearer` |
| E55 | — | Login con body malformado o vacío / método HTTP no soportado / ruta inexistente con token válido / excepción inesperada | 400 / 405 / 404 / 500, todos en Problem Details; el 500 sin detalles internos |
| E56 | El circuit breaker cerrado | SWAPI responde 429 varias veces | El 429 no se registra en el breaker (ni como fallo ni como éxito) |
| E57 | SWAPI responde 200 con envoltorio incompleto (sin `results`, `total_records` o `total_pages`; sin `result`) o con un item sin `properties` o sin `uid` | Cualquier consulta | 502 Problem Details; la respuesta no se cachea |
| E58 | — | `?name=Luke` y luego `?name=luke` / `name` de más de 100 caracteres / `username` o `password` de más de 72 caracteres | Una sola llamada a SWAPI (misma entrada de caché) / 400 / 400 |
| E59 | — | Una respuesta 502, 503 o 500 | Queda un log (`warn` para 502 y 503, `error` para 500) con el `requestId` |
| E60 | Valores `"1,2,3"`, un número que excede `Double`, y una relación `"/"` | Se mapean | `null`, `null` y la relación se descarta |

## Decisiones de diseño
- Los usuarios demo (incluido `admin`) quedan activos en producción a propósito, para que quien revisa pruebe la autorización en la URL desplegada; ver el [README](../README.md#usuarios-demo-en-producción) y el [ADR 0004](adr/0004-autenticacion-jwt.md).
- Con el breaker abierto, `Retry-After` informa la espera configurada completa: es una cota superior segura; ver el [ADR 0005](adr/0005-resiliencia.md).

## No unit-testeables
| ID | Por qué no | Cómo se valida |
|---|---|---|
| N1 | Imagen Docker | `docker build` y `docker run` locales, y smoke con `curl` contra `/actuator/health` y el login |
| N2 | Deploy en Render (cuenta del autor) | Smoke con `curl` contra la URL pública: health, login y `GET /people` |
| N3 | CI en GitHub Actions | Pipeline en verde en el primer push a `main` |
| N4 | Colección de Bruno | `bruno run` contra la app local en verde; el login guarda el token y los demás requests lo usan |
| N5 | Documentación (`README` con "Probalo en 2 minutos", ADR, Javadoc, OpenAPI) | Revisión manual: seguir el `README` desde un clone limpio, y comparar Swagger UI contra respuestas reales |
| N6 | Cobertura | Reporte de JaCoCo en `./mvnw verify` con piso por paquete de 80 % de líneas y 70 % de ramas en `domain*` y `application*` (falla el build si no se cumple) |
