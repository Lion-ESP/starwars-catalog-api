# eclipse-temurin:21.0.12_8-jdk-noble
FROM eclipse-temurin:21.0.12_8-jdk-noble@sha256:4d271cd5e0624598cf563342f47281b09cb364bc13acbbd7251f49f83470018d AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI (build job), not while building the image.
RUN ./mvnw -B -q package -DskipTests \
    && cp target/starwars-catalog-api-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination /layers

# eclipse-temurin:21.0.12_8-jre-noble
FROM eclipse-temurin:21.0.12_8-jre-noble@sha256:7739f0ffce786528961eea6bf46d9610ee968ac6127c9b2e93494757bdecce9f
RUN groupadd --system app && useradd --system --gid app --no-create-home app \
    && mkdir /app && chown app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /layers/dependencies/ ./
COPY --from=build --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /layers/application/ ./
USER app
# CDS training run: starts the context once and archives the loaded classes, which roughly halves startup on a 0.1 CPU instance.
# The throwaway secret only satisfies startup validation and is not kept in the image.
RUN JWT_SECRET="$(head -c 48 /dev/urandom | base64)" \
    java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar
EXPOSE 8080
# A 65% heap leaves ~180 MB of 512 MB for metaspace, code cache and buffers; exiting on OOM lets the platform restart the container.
# SerialGC and C1-only keep CPU and memory low on a 0.1 CPU free instance.
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-XX:MaxRAMPercentage=65", "-XX:+ExitOnOutOfMemoryError", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "app.jar"]
