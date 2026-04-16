#!/usr/bin/env bash
# Build the Docker image, run the standalone mock-server container, run REST E2E tests, then stop.
#
# Usage (from mock-server repo root):
#   ./scripts/e2e-against-docker.sh
#
# Env:
#   MOCK_SERVER_E2E_PORT   host port (default 18080)
#   MOCK_SERVER_E2E_IMAGE  image tag (default spec0/mock-server:e2e-local)
#   SKIP_UI_BUILD          if 1, skip "cd ui && npm run build" when ui/out already exists
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${MOCK_SERVER_E2E_PORT:-18080}"
IMAGE="${MOCK_SERVER_E2E_IMAGE:-spec0/mock-server:e2e-local}"
CONTAINER_NAME="${MOCK_SERVER_E2E_CONTAINER:-spec0-mock-server-e2e}"

MVN="${ROOT}/mvnw"
if [[ ! -x "$MVN" ]]; then
  MVN="mvn"
fi

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> Building fat JAR (standalone + dependencies)"
if [[ -d "$ROOT/ui/out" ]] || [[ "${SKIP_UI_BUILD:-}" == "1" ]]; then
  "$MVN" -q -pl standalone -am package -DskipTests -Dskip.ui.copy=true
else
  if [[ ! -d "$ROOT/ui/node_modules" ]]; then
    (cd "$ROOT/ui" && npm ci)
  fi
  (cd "$ROOT/ui" && npm run build)
  "$MVN" -q -pl standalone -am package -DskipTests
fi

echo "==> Building Docker image: $IMAGE"
docker build -t "$IMAGE" .

echo "==> Starting container $CONTAINER_NAME on host port $PORT"
cleanup
docker run -d \
  --name "$CONTAINER_NAME" \
  -p "${PORT}:8080" \
  -v spec0-mock-e2e-data:/data \
  "$IMAGE"

BASE_URL="http://127.0.0.1:${PORT}"
echo "==> Waiting for health at $BASE_URL/actuator/health"
for _ in $(seq 1 60); do
  if curl -sf "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
    echo "==> Server is UP"
    break
  fi
  sleep 2
done
if ! curl -sf "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
  echo "ERROR: health check failed after wait" >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

echo "==> Running E2E tests against $BASE_URL"
"$MVN" -q -f "$ROOT/e2e-tests/pom.xml" test "-Dmock.server.baseUrl=${BASE_URL}"

echo "==> E2E tests passed"
