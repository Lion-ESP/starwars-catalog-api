# ADR 0001 — Stack

**Estado:** Aceptada

## Contexto
Hay que construir una API REST en Java 21 con Spring Boot que consume una API externa (swapi.tech). Casi todo el tiempo de un request se va esperando I/O de SWAPI, y el servicio se despliega en un plan gratuito de 512 MB de RAM.

## Decisión
- Java 21, Spring Boot 4.1 y Maven con wrapper (`./mvnw`), de modo que el build no depende de nada instalado.
- Servidor MVC (bloqueante) con **virtual threads** (`spring.threads.virtual.enabled=true`): el código se escribe de forma secuencial y cada request en espera de SWAPI no ocupa un thread de plataforma.
- **`RestClient`** con el cliente HTTP del JDK y timeouts de conexión y lectura configurables.

## Alternativas consideradas
- **WebFlux y `WebClient`:** resuelve el mismo problema de espera de I/O, pero obliga a un modelo reactivo en todo el código, con más complejidad para tests y resiliencia sin beneficio medible a esta escala. Con virtual threads no hace falta.
- **Gradle:** equivalente para este proyecto; se eligió Maven por simplicidad de configuración y porque es lo más habitual en el ecosistema Spring.
- **Pool de threads de plataforma:** exigiría dimensionarlo, y en 512 MB un pool grande es un costo.

## Consecuencias
- Código simple, bloqueante y fácil de testear; el consumo de memoria se mantiene bajo (la imagen usa unos 171 MiB bajo un límite de 512 MB).
- Se depende de Spring Boot 4, reciente: algunas librerías (por ejemplo WireMock) se usan en variantes compatibles, y Jackson 3 cambia paquetes respecto de versiones anteriores.
