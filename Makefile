# Open-source mock server — contributor shortcuts (requires Java 17, Maven, Node 18+)
# See README.md for env vars and Docker. Run `make help` for targets.

.PHONY: help dev-backend dev-ui generate-types e2e-docker

help:
	@echo "Mock server — local development"
	@echo "  make dev-backend   Spring Boot API http://localhost:8080 (H2: standalone/target/)"
	@echo "  make dev-ui        Next.js http://localhost:3000 (API on 8080 unless ui/.env.local)"
	@echo "  make generate-types  Regenerate ui/lib/api/generated.ts from openapi.yaml"
	@echo "  make e2e-docker      Build image, run container, REST E2E tests (see scripts/e2e-against-docker.sh)"
	@echo ""
	@echo "Shell equivalents: scripts/dev-backend.sh | scripts/dev-ui.sh"

# Spring Boot API on http://localhost:8080 (H2 file under standalone/target/)
dev-backend:
	cd standalone && mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:$$(pwd)/target/spec0-mock-dev;DB_CLOSE_ON_EXIT=FALSE"

# Next.js UI on http://localhost:3000 — talks to API on :8080 by default (see ui/.env.example)
dev-ui:
	cd ui && npm run dev

generate-types:
	cd ui && npm run generate-types

e2e-docker:
	./scripts/e2e-against-docker.sh
