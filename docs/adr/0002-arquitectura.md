# ADR 0002 — Arquitectura

**Estado:** Aceptada

## Contexto
El servicio es de lectura, sin base de datos propia y con poca lógica de negocio. Lo complejo está en el borde con SWAPI: formatos irregulares, paginación inconsistente, cupo de requests y caídas. Cuatro entidades (people, films, starships, vehicles) se comportan casi igual.

## Decisión
- **Ports and adapters solo en el borde con SWAPI.** `application` define los puertos de salida (`CatalogPort<T>`, `CatalogCache`) y `adapter/out` los implementa.
- `domain` y `application` no usan Spring ni Jackson: son records y clases simples; los beans se arman en `CatalogConfig`. Lo verifica ArchUnit (`ArchitectureTest`) en cada build.
- **Un solo caso de uso (`CatalogService<T>`) y un solo adaptador (`SwapiCatalogAdapter<T>`) genéricos** para las cuatro entidades. Lo que varía vive en `SwapiResource` y en un mapper por entidad. Los cuatro controllers son finos para que OpenAPI tipe cada respuesta.

## Alternativas consideradas
- **Capas clásicas (controller, service, repository):** acoplan la lógica al transporte y al formato de SWAPI, y dificultan testear sin HTTP.
- **Arquitectura hexagonal completa** (puertos de entrada y de salida para todo): agrega interfaces y mapeos sin una lógica de negocio que lo justifique en un servicio de lectura.
- **Una clase por entidad** (servicio y adaptador por recurso): cuadruplica código casi idéntico; sumar un recurso se reduce a un valor de `SwapiResource`, un record, un mapper, un bean y un controller.

## Consecuencias
- Los casos de uso se testean con un fake del puerto, sin red ni Spring, y el origen de datos se puede reemplazar sin tocar la lógica.
- Sumar un recurso no modifica el servicio ni el cliente.
- Costo: una capa de indirección (puertos) y la disciplina de mantener las reglas de dependencia, que ArchUnit hace cumplir.
