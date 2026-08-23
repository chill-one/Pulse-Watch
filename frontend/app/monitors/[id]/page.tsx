import Link from "next/link";
import { notFound } from "next/navigation";
import DeleteMonitorButton from "../../../components/DeleteMonitorButton";

import LatencySparkline from "../../../components/LatencySparkline";

import {
  getMonitor,
  getRecentChecks,
  getRecentIncidents,
} from "../../../lib/api";

function formatDuration(seconds: number | null) {
  if (seconds === null) {
    return "Ongoing";
  }

  if (seconds < 60) {
    return `${seconds}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  return `${minutes}m ${remainingSeconds}s`;
}

export default async function MonitorPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  const monitor = await getMonitor(id);

  if (!monitor) {
    notFound();
  }

  const [checks, incidents] = await Promise.all([
    getRecentChecks(id, 50),
    getRecentIncidents(id, 10),
  ]);

  const latestCheck = checks[0] ?? null;

  return (
    <main>
      <Link href="/" className="back-link">
        ← Back to dashboard
      </Link>

    <div className="monitor-actions">
    <span
        className={`status status-${monitor.status.toLowerCase()}`}
    >
        {monitor.status}
    </span>

    <Link
        href={`/monitors/${monitor.id}/edit`}
        className="secondary-button"
    >
        Edit Monitor
    </Link>

    <DeleteMonitorButton
        monitorId={monitor.id}
        monitorName={monitor.name}
    />
    </div>

      <div className="stat-grid">
        <div className="stat-card">
          <span className="stat-label">
            Current latency
          </span>

          <strong>
            {latestCheck
              ? `${latestCheck.latencyMs} ms`
              : "—"}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">
            Last response
          </span>

          <strong>
            {latestCheck
              ? latestCheck.statusCode ??
                latestCheck.error ??
                "Unknown"
              : "—"}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">
            Failures
          </span>

          <strong>
            {monitor.consecutiveFailureCount}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">
            Last checked
          </span>

          <strong>
            {latestCheck
              ? new Date(
                  latestCheck.checkedAt
                ).toLocaleString()
              : "Never"}
          </strong>
        </div>
      </div>

      <section className="detail-section">
        <div className="section-header">
          <h2>Latency history</h2>

          <span>
            Last {checks.length} checks
          </span>
        </div>

        <div className="details-chart">
          <LatencySparkline checks={checks} />
        </div>
      </section>

      <section className="detail-section">
        <h2>Recent checks</h2>

        {checks.length === 0 ? (
          <p className="empty-state">
            No checks recorded yet.
          </p>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Checked at</th>
                  <th>Response</th>
                  <th>Latency</th>
                </tr>
              </thead>

              <tbody>
                {checks.map((check) => (
                  <tr key={check.id}>
                    <td>
                      {new Date(
                        check.checkedAt
                      ).toLocaleString()}
                    </td>

                    <td>
                      {check.statusCode ??
                        check.error ??
                        "Unknown"}
                    </td>

                    <td>
                      {check.latencyMs} ms
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="detail-section">
        <h2>Incident history</h2>

        {incidents.length === 0 ? (
          <p className="empty-state">
            No incidents recorded.
          </p>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Started</th>
                  <th>Ended</th>
                  <th>Duration</th>
                </tr>
              </thead>

              <tbody>
                {incidents.map((incident) => (
                  <tr key={incident.id}>
                    <td>
                      {new Date(
                        incident.startedAt
                      ).toLocaleString()}
                    </td>

                    <td>
                      {incident.endedAt
                        ? new Date(
                            incident.endedAt
                          ).toLocaleString()
                        : "Ongoing"}
                    </td>

                    <td>
                      {formatDuration(
                        incident.durationSeconds
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}