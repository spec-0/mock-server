#!/usr/bin/env bash
# Run the Next.js dev server (port 3000). Expects the API at http://localhost:8080
# unless ui/.env.local overrides NEXT_PUBLIC_MOCK_SERVER_API_URL.
# Usage: from apps/mock-server: ./scripts/dev-ui.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/ui"
exec npm run dev
