#!/usr/bin/env bash
set -euo pipefail

KIBANA_URL="${KIBANA_URL:-http://localhost:5601}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

curl --fail --silent --show-error \
  --request POST \
  --header 'kbn-xsrf: true' \
  --form "file=@${SCRIPT_DIR}/dashboard.ndjson" \
  "${KIBANA_URL}/api/saved_objects/_import?overwrite=true"

echo
echo "Imported dashboard: SSISW — giełda samochodowa"
