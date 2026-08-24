#!/usr/bin/env bash

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
TARGET_URL="${TARGET_URL:-http://host.docker.internal:9000}"
COUNT="${COUNT:-200}"

echo "Creating $COUNT PulseWatch load-test monitors..."

for i in $(seq 1 "$COUNT"); do
  NAME=$(printf "LoadTest-%03d" "$i")

  STATUS=$(
    curl -sS \
      -o /dev/null \
      -w "%{http_code}" \
      -X POST "$API_URL/monitors" \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"$NAME\",
        \"url\": \"$TARGET_URL\",
        \"checkIntervalSeconds\": 60,
        \"timeoutSeconds\": 2
      }"
  )

  if [ "$STATUS" != "201" ]; then
    echo "Failed to create $NAME. HTTP status: $STATUS"
    exit 1
  fi

  if (( i % 25 == 0 )); then
    echo "Created $i/$COUNT monitors"
  fi
done

echo "Finished creating $COUNT monitors."