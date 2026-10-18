# ADR 0002 — Arquitectura

**Estado:** Aceptada

## Contexto
El servicio es de lectura, sin base de datos propia y con poca lógica de negocio. Lo complejo está en el borde con SWAPI: formatos irregulares, paginación inconsistente, cupo de requests y caídas. Cuatro entidades (people, films, starships, vehicles) se comportan casi igual.

## Decisión
- **Hexagonal acotada al borde con SWAPI.** `application` define los puertos de salida (`CatalogPort<T>`, `CatalogCache`) en `application/port/out` y `adapter/out` los implementa. No hay puertos de entrada: los controllers llaman directo a los casos de uso.
- **Las excepciones de puerto viven con el puerto.** `CatalogUnavailableException` (con `retryAfter()`) y `CatalogUpstreamException` están en `application/port/out`; las `Swapi*Exception` las extienden. La capa web traduce solo esas dos a 503 y 502, sin conocer a SWAPI.
- `domain` y `application` no usan Spring ni Jackson: son records y clases simples; los beans se arman en `CatalogConfig`.
- **Cinco reglas de ArchUnit** (`ArchitectureTest`) lo hacen cumplir en cada build: (1) `domain` no depende de Spring, Jackson, `adapter` ni `application`; (2) `application` no depende de Spring, Jackson ni `adapter`; (3) `domain` y `application` no dependen de Jakarta, Resilience4j, Caffeine, `security` ni `config`; (4) `adapter.in` no depende de `adapter.out`; (5) `adapter.out` no depende de `adapter.in` ni de `security`. El login vive en `security/LoginService`, fuera de los casos de uso del catálogo.
- **Un solo caso de uso (`CatalogService<T>`) y un solo adaptador (`SwapiCatalogAdapter<T>`) genéricos** para las cuatro entidades. Lo que varía vive en `SwapiResource` y en un mapper por entidad. Los cuatro controllers son finos para que OpenAPI tipe cada respuesta.

## Alternativas consideradas
- **Capas clásicas (controller, service, repository):** acoplan la lógica al transporte y al formato de SWAPI, y dificultan testear sin HTTP.
- **Hexagonal con puertos de entrada y de salida para todo:** agrega interfaces y mapeos sin una lógica de negocio que lo justifique en un servicio de lectura; el único borde con comportamiento propio es SWAPI.
- **Una clase por entidad** (servicio y adaptador por recurso): cuadruplica código casi idéntico; sumar un recurso se reduce a un valor de `SwapiResource`, un record, un mapper, un bean y un controller.

## Consecuencias
- Los casos de uso se testean con un fake del puerto, sin red ni Spring, y el origen de datos se puede reemplazar sin tocar la lógica.
- Sumar un recurso no modifica el servicio ni el cliente.
- Costo: una capa de indirección (puertos) y la disciplina de mantener las reglas de dependencia, que ArchUnit hace cumplir.
