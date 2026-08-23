import type { Monitor } from "../types/monitor";

interface MonitorCardProps {
  monitor: Monitor;
}

export default function MonitorCard({
  monitor,
}: MonitorCardProps) {
  return (
    <article>
      <h3>{monitor.name}</h3>

      <p>{monitor.status}</p>

      <p>{monitor.url}</p>

      <p>
        Check interval: {monitor.checkIntervalSeconds} seconds
      </p>

      <p>
        Timeout: {monitor.timeoutSeconds} seconds
      </p>

      <p>
        Consecutive failures:{" "}
        {monitor.consecutiveFailureCount}
      </p>
    </article>
  );
}