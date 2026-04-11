# Runtime image only — the Spring Boot executable JAR must be built *before* `docker build`.
#
# Typical flow (GitHub Actions or locally):
#   1. cd ui && npm ci && npm run build
#   2. mvn -pl standalone -am package -DskipTests
#   3. docker build -t spec0/mock-server:latest .
#
# Build context: the `mock-server/` directory (repo root containing `standalone/target/...`).

FROM eclipse-temurin:17-jre-jammy

VOLUME /data
WORKDIR /app

# Exactly one JAR must match (Spring Boot repackage with classifier `exec`).
COPY standalone/target/mock-server-standalone-*-exec.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
