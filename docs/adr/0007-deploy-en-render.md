# ADR 0007 — Deploy en Render

**Estado:** Aceptada

## Contexto
El enunciado pide la API desplegada en un servidor gratuito para que el revisor la pruebe. Hace falta soporte para Java, sin tarjeta de crédito, y que el artefacto sea el mismo que se corre en local.

## Decisión
- **Render con el plan gratuito, desplegando desde el `Dockerfile`** (multi-stage, usuario no root) mediante el blueprint `render.yaml`: genera `JWT_SECRET`, activa el perfil `prod` (logs ECS) y usa `/actuator/health` como health check.
- Ajustes de JVM para 512 MB: `-XX:MaxRAMPercentage=75`, `-XX:+UseSerialGC`, `-XX:TieredStopAtLevel=1` y `-Xss512k`. La imagen arranca en unos 2 s y usa unos 171 MiB bajo un límite de 512 MB.

## Alternativas consideradas
- **Heroku:** ya no tiene plan gratuito.
- **Vercel:** no ejecuta aplicaciones Java.
- **Koyeb, Fly.io, Cloud Run, Oracle Cloud:** alternativas viables; se descartaron porque requieren tarjeta de crédito, o un alta y una configuración más pesadas que las de Render para una demo.
- **Deploy sin contenedor (buildpack):** el Dockerfile ya existe para correr en local y garantiza el mismo artefacto.

## Consecuencias
- Despliegue reproducible y sin costo, con un blueprint versionado.
- **Arranque en frío:** el servicio se duerme a los 15 minutos sin tráfico y el primer request tarda alrededor de un minuto; el README lo advierte.
- Recursos limitados (512 MB y 0,1 CPU): sin margen para cargas altas, aceptable para una demo.
- Swagger queda habilitado en `prod` a propósito, porque es la forma en que el revisor prueba la API.
