# ADR 0004 — Autenticación con JWT

**Estado:** Aceptada

## Contexto
El enunciado pide login y control de acceso por roles. Es una demo que debe poder probarse sin registrarse y sin base de datos, y sin que un token inválido rompa las rutas públicas (health, Swagger).

## Decisión
- **JWT HS256** emitido con `NimbusJwtEncoder` y validado con el resource server de Spring Security, sin librerías adicionales. Claims `iss`, `sub`, `iat`, `exp` y `roles`.
- **Usuarios predefinidos** por configuración (hash BCrypt), con roles `USER` (lee el catálogo) y `ADMIN` (además vacía la caché y ve métricas). Las contraseñas demo son públicas a propósito y sobrescribibles.
- **Dos cadenas de filtros** sin estado: una pública (login, health, Swagger y OpenAPI) sin resource server, de modo que un bearer inválido enviado a una ruta pública se ignora; otra protegida que valida el JWT y los roles. Todo lo no declarado se deniega.
- El login usa `DaoAuthenticationProvider`, que compara contra un hash de relleno cuando el usuario no existe, para que el tiempo de respuesta no permita enumerar usuarios. Ambos casos devuelven el mismo mensaje.
- El secreto llega por `JWT_SECRET`; si falta o tiene menos de 32 bytes, la aplicación **falla al arrancar**.

## Alternativas consideradas
- **Sesiones con cookies:** requieren estado en el servidor y no encajan con una API consumida por scripts y Swagger.
- **Biblioteca JWT externa (jjwt, auth0):** innecesaria, Spring Security ya incluye Nimbus.
- **Proveedor externo (Keycloak, Auth0):** infraestructura desproporcionada para una demo.
- **RS256 con par de claves:** útil con varios consumidores del token; aquí emisor y validador son el mismo servicio.

## Consecuencias
- Cero estado y cero dependencias extra; el revisor puede probar con las credenciales del README.
- Sin refresh tokens ni revocación: un token vale hasta que vence (1 hora por defecto).
- Sin protección contra fuerza bruta en el login; queda documentado como mejora.
