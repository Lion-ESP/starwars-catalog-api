# ADR 0007 — Deploy en Render

**Estado:** Aceptada

## Contexto
El enunciado pide la API desplegada en un servidor gratuito para que el revisor la pruebe. Hace falta soporte para Java, sin tarjeta de crédito, y que el artefacto sea el mismo que se corre en local.

## Decisión
- **Render con el plan gratuito, desplegando desde el `Dockerfile`** (multi-stage, usuario no root) mediante el blueprint `render.yaml`: genera `JWT_SECRET`, activa el perfil `prod` (logs ECS) y usa `/actuator/health` como health check.
- Ajustes de JVM para 512 MB y 0,1 CPU: `-XX:MaxRAMPercentage=65`, `-XX:+ExitOnOutOfMemoryError` (para que la plataforma reinicie el contenedor), `-XX:+UseSerialGC`, `-XX:TieredStopAtLevel=1` y `-Xss512k`. Medido localmente con `--memory 512m`, el contenedor usa alrededor de 160 MiB en reposo y arranca en unos 2 s con CPU completa y en unos 45 s limitado a 0,1 CPU (la fracción del plan gratuito); el health tarda unos 50 s en responder.

## Alternativas consideradas
- **Heroku:** el enunciado lo sugiere, pero al momento de decidir no encontré un plan gratuito.
- **Vercel:** el enunciado lo menciona, pero está pensado para frontends y funciones serverless, no para un servicio Java persistente.
- **Koyeb, Fly.io, Cloud Run, Oracle Cloud:** alternativas viables; al momento de decidir, los planes gratuitos que evalué pedían tarjeta o no ofrecían una instancia gratuita, o requerían un alta y una configuración más pesadas que las de Render para una demo.
- **Deploy sin contenedor (buildpack):** el Dockerfile ya existe para correr en local y garantiza el mismo artefacto.

## Consecuencias
- Despliegue reproducible y sin costo, con un blueprint versionado.
- **Arranque en frío:** el servicio se duerme a los 15 minutos sin tráfico y el primer request tarda alrededor de un minuto (el arranque con 0,1 CPU es de unos 45 s); el README lo advierte.
- El `Dockerfile` no define `HEALTHCHECK`: el chequeo de salud lo hacen Render (`healthCheckPath`) y el smoke del CI contra `/actuator/health`.
- Recursos limitados (512 MB y 0,1 CPU): sin margen para cargas altas, aceptable para una demo.
- Swagger queda habilitado en `prod` a propósito, porque es la forma en que el revisor prueba la API.
