import type { Monitor } from "../types/monitor";
import type { CheckResult } from "../types/checkResult";

interface MonitorCardProps {
  monitor: Monitor;
  latestCheck: CheckResult | null;
}

function getStatusClass(status: Monitor["status"]) {
  return `status status-${status.toLowerCase()}`;
}

export default function MonitorCard({
  monitor, latestCheck }: MonitorCardProps) {
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

      <div className="monitor-details">
        <p>
            Latest latency:{" "}
            {latestCheck
            ? `${latestCheck.latencyMs} ms`
            : "No checks yet"}
        </p>

        <p>
            Last checked:{" "}
            {latestCheck
            ? new Date(latestCheck.checkedAt).toLocaleString()
            : "Never"}
        </p>

        <p>
            Last response:{" "}
            {latestCheck
                ? latestCheck.statusCode ??
                latestCheck.error ??
                "Unknown"
                : "No checks yet"}
        </p>
     </div>

    </article>
  );
}