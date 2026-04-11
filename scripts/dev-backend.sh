#!/usr/bin/env bash
# Run the mock server API locally (port 8080 by default).
# Usage: from apps/mock-server: ./scripts/dev-backend.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/standalone"
exec mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:${ROOT}/standalone/target/spec0-mock-dev;DB_CLOSE_ON_EXIT=FALSE"
