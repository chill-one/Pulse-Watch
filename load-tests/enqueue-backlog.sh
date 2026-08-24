#!/usr/bin/env bash

set -euo pipefail

PROJECT="pulsewatch-perf"
BATCHES="${BATCHES:-25}"

echo "Preparing $BATCHES batches..."
echo "$((BATCHES * 200)) total tasks expected."

for i in $(seq 1 "$BATCHES"); do

  docker compose -p "$PROJECT" exec -T postgres \
    psql -U pulsewatch -d pulsewatch \
    -c "
      UPDATE monitor
      SET next_check_at = NOW() - INTERVAL '1 second'
      WHERE name LIKE 'LoadTest-%';
    " >/dev/null

  # Scheduler polls every ~5 seconds.
  sleep 6

  TOTAL=$(
    docker compose -p "$PROJECT" exec -T rabbitmq \
      rabbitmqctl list_queues name messages 2>/dev/null |
      awk '$1 == "pulsewatch.check.tasks" {print $2}'
  )

  echo "Batch $i/$BATCHES -> queue: ${TOTAL:-unknown}"
done