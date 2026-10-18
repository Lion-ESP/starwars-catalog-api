# ADR 0003 — Proxy con caché

**Estado:** Aceptada

## Contexto
SWAPI tiene un cupo de 100 requests por ventana, su paginación solo funciona en `people`, `starships` y `vehicles` (`films` devuelve las 6 películas sin paginar), la búsqueda por nombre no se pagina y una página fuera de rango devuelve la última en lugar de vacío. Los datos casi no cambian (82 people, 36 starships, 39 vehicles, 6 films).

## Decisión
- **Proxy con caché:** cada consulta se traduce a SWAPI.
- **Paginación delegada** a SWAPI (con `expanded=true`, para traer los datos completos en una sola llamada) donde la soporta, y **en memoria** para `films` y para las búsquedas por nombre. Una página mayor al total devuelve vacía con los totales reales.
- **Caché Caffeine** con TTL de 1 hora y máximo de 500 entradas, con `sync = true` (requests concurrentes idénticos hacen una sola llamada) y el **404 cacheado**. Las excepciones nunca se cachean.
- **Las respuestas inválidas de SWAPI no se cachean:** el cliente valida el envoltorio (por ejemplo, un `results` ausente o un item sin `uid`) dentro del método cacheado y lanza una excepción, que termina en un 502 y no deja nada en la caché.
- **La clave de caché de la búsqueda se normaliza a minúsculas:** `?name=Luke` y `?name=luke` comparten entrada y hacen una sola llamada a SWAPI.
- La caché envuelve a la resiliencia: un hit no consume cupo ni cuenta en el circuit breaker. Un endpoint de `ADMIN` la vacía.

## Alternativas consideradas
- **Snapshot en memoria al arrancar:** respuestas instantáneas, pero el arranque depende de SWAPI (y en frío ya es lento), consume cupo en cada arranque y los datos quedan obsoletos hasta el próximo reinicio.
- **Sin caché:** cada request consume cupo; con 100 por ventana se agota rápido y la API se cae sola.
- **Persistir en una base propia:** agrega infraestructura y sincronización que no se justifican para datos casi estáticos.

## Consecuencias
- Respuestas rápidas y poco consumo de cupo; el 404 repetido no vuelve a SWAPI.
- Los datos pueden tener hasta 1 hora de antigüedad y la caché es local a cada instancia (con varias instancias haría falta una caché compartida).
- **Trade-off conocido: pinning de virtual threads.** `@Cacheable(sync = true)` carga el valor dentro de un `synchronized`, y en Java 21 eso fija el hilo portador de un virtual thread mientras espera a SWAPI. Con cuatro recursos y un cupo de 100 requests el riesgo es bajo; si creciera, se resolvería con una caché asíncrona (por ejemplo, `AsyncCache` de Caffeine).
- La traducción entre el modelo de paginación de la API y el de SWAPI queda encapsulada en el adaptador.
