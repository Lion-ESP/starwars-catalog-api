FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI (build job), not while building the image.
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/starwars-catalog-api-*.jar extract --layers --launcher --destination /layers

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /layers/dependencies/ ./
COPY --from=build --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /layers/application/ ./
USER app
EXPOSE 8080
# A 75% heap leaves ~128 MB of a 512 MB container for metaspace, threads and native buffers;
# SerialGC and the C1-only compiler keep CPU and memory low on a 0.1 CPU free instance.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "org.springframework.boot.loader.launch.JarLauncher"]
