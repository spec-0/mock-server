# Runtime image only — the Spring Boot executable JAR must be built *before* `docker build`.
#
# Typical flow (GitHub Actions or locally):
#   1. cd ui && npm ci && npm run build
#   2. mvn -pl standalone -am package -DskipTests
#   3. docker build -t spec0/mock-server:latest .
#
# Build context: the `mock-server/` directory.

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser \
    && useradd -r -g appuser appuser

WORKDIR /app
RUN mkdir -p /data && chown appuser:appuser /data /app

COPY --chown=appuser:appuser standalone/target/mock-server-standalone-*-exec.jar app.jar

VOLUME /data
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

USER appuser
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
