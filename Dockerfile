# eclipse-temurin:21.0.12_8-jdk-noble
FROM eclipse-temurin:21.0.12_8-jdk-noble@sha256:4d271cd5e0624598cf563342f47281b09cb364bc13acbbd7251f49f83470018d AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI (build job), not while building the image.
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/starwars-catalog-api-*.jar extract --layers --launcher --destination /layers

# eclipse-temurin:21.0.12_8-jre-noble
FROM eclipse-temurin:21.0.12_8-jre-noble@sha256:7739f0ffce786528961eea6bf46d9610ee968ac6127c9b2e93494757bdecce9f
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /layers/dependencies/ ./
COPY --from=build --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /layers/application/ ./
USER app
EXPOSE 8080
# A 65% heap leaves ~180 MB of 512 MB for metaspace, code cache and buffers; exiting on OOM lets the platform restart the container.
# SerialGC and C1-only keep CPU and memory low on a 0.1 CPU free instance.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+ExitOnOutOfMemoryError", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "org.springframework.boot.loader.launch.JarLauncher"]
