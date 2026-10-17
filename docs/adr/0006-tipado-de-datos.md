# ADR 0006 — Tipado de datos

**Estado:** Aceptada

## Contexto
SWAPI devuelve casi todo como texto: números (`"172"`, `"843,342"`, `"36.8 "`, `"1000000000000"`), valores desconocidos (`"unknown"`, `"n/a"`, `"none"`, vacío, rangos como `"30-165"`), fechas y relaciones como URLs absolutas.

## Decisión
- Los atributos numéricos se exponen como **números**: enteros de 64 bits (`Long`) y decimales (`Double`). El mapeo quita espacios y separadores de miles.
- Todo valor desconocido o no interpretable se expone como **`null`**. Lo mismo vale para una fecha que no parsea.
- Las relaciones se exponen como **ids** (`"https://www.swapi.tech/api/films/1"` pasa a `"1"`); `homeworld` es `homeworldId`; las listas vacías son `[]`.
- **Nunca un error por un dato raro:** un campo que no se puede interpretar queda en `null` y el resto de la entidad se mapea igual. Los DTO ignoran propiedades desconocidas.
- El parseo vive en funciones puras (`SwapiValues`), cubiertas con tests unitarios.

## Alternativas consideradas
- **Reenviar los textos tal cual:** traslada el parseo a cada consumidor y no permite ordenar ni comparar.
- **Fallar ante un dato inesperado:** un solo registro irregular rompería listados enteros.
- **Expandir relaciones a objetos completos:** multiplica las llamadas a SWAPI y el cupo; queda como mejora.
- **Usar `int`:** `1000000000000` excede su rango.

## Consecuencias
- Un contrato tipado y consistente, útil para el cliente y para la documentación OpenAPI.
- Se pierde la distinción entre "desconocido" y "no aplica": ambos son `null`.
- Un rango como `"30-165"` se expone como `null` aunque tenga información.
