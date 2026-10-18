# Decisiones de arquitectura (ADR)

Registros cortos de las decisiones principales del proyecto: contexto, decisión, alternativas descartadas y consecuencias. Todas están aceptadas.

| ADR | Título |
|---|---|
| [0001](0001-stack.md) | Stack: Java 21, Spring Boot 4.1, Maven, virtual threads y `RestClient` |
| [0002](0002-arquitectura.md) | Arquitectura hexagonal acotada al borde con SWAPI, con reglas de ArchUnit |
| [0003](0003-proxy-con-cache.md) | Proxy con caché en lugar de snapshot en memoria; pinning con `sync = true` |
| [0004](0004-autenticacion-jwt.md) | Autenticación con JWT HS256; usuarios demo también en producción |
| [0005](0005-resiliencia.md) | Resiliencia: caché, circuit breaker y retry; 502 vs 503; `429` y `Retry-After` |
| [0006](0006-tipado-de-datos.md) | Tipado de datos de SWAPI |
| [0007](0007-deploy-en-render.md) | Deploy en Render con Docker; arranque y memoria |
