import type { Monitor } from "../types/monitor";

interface MonitorCardProps {
  monitor: Monitor;
}

function getStatusClass(status: Monitor["status"]) {
  return `status status-${status.toLowerCase()}`;
}

export default function MonitorCard({
  monitor,
}: MonitorCardProps) {
  return (
    <article className="monitor-card">

      <div className="monitor-header">
        <h3>{monitor.name}</h3>

        <span className={getStatusClass(monitor.status)}>
          {monitor.status}
        </span>
      </div>

      <p className="monitor-url">
        {monitor.url}
      </p>

      <div className="monitor-details">

        <p>
          Check interval:{" "}
          {monitor.checkIntervalSeconds} seconds
        </p>

        <p>
          Timeout: {monitor.timeoutSeconds} seconds
        </p>

        <p>
          Consecutive failures:{" "}
          {monitor.consecutiveFailureCount}
        </p>

      </div>

    </article>
  );
}