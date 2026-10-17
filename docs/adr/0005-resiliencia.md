# ADR 0005 — Resiliencia

**Estado:** Aceptada

## Contexto
SWAPI es un servicio gratuito de terceros: puede responder 5xx, demorarse, rechazar conexiones o devolver 429 al agotar el cupo. La API propia debe degradarse de forma predecible y decirle al cliente cuándo reintentar.

## Decisión
- **Orden caché → circuit breaker → retry → llamada HTTP.** Un hit de caché no llega al breaker; una secuencia de reintentos cuenta como una sola llamada. Resilience4j se usa de forma programática (sin AOP).
- **Se reintenta** (3 intentos, espera de 200 ms con backoff exponencial): respuestas 5xx y fallas de conexión.
- **No se reintenta:** timeout de lectura (ya consumió 5 s y reintentar multiplica la espera), 404 (es una respuesta válida), 429 (reintentar empeora el cupo) y respuestas inválidas (volver a pedir no las arregla).
- **Circuit breaker** con ventana de 10 llamadas, mínimo 5 y umbral del 50 %, abierto 30 s. Registra 5xx, timeouts, fallas de conexión y respuestas inválidas; ignora 404 y 429.
- **502 vs 503:** `502 Bad Gateway` cuando SWAPI falla de forma persistente (5xx) o responde algo inesperado; `503 Service Unavailable` con `Retry-After` cuando está temporalmente inutilizable (timeout, conexión, cupo, circuito abierto). El valor sale del header `x-ratelimit-reset`, de la espera del breaker o del default configurado (30 s).

## Alternativas consideradas
- **Retry por fuera del breaker o breaker por fuera de la caché:** los hits contarían como llamadas y los reintentos inflarían el conteo de fallas.
- **Reintentar todo:** multiplica latencias en timeouts y agrava el cupo en 429.
- **Sin circuit breaker:** cada request esperaría el timeout completo mientras SWAPI está caído.

## Consecuencias
- La API falla rápido y de forma explicable, con `Retry-After` accionable.
- Con el breaker abierto, las consultas no cacheadas devuelven 503 aunque SWAPI ya se haya recuperado hasta la próxima ventana semiabierta.
- Los parámetros son configurables, pero un ajuste malo puede abrir el circuito de más o tardar en abrirlo.
