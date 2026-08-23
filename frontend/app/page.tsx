import MonitorCard from "../components/MonitorCard";
import Link from "next/link";
import AutoRefresh from "../components/AutoRefresh";

import {
  getMonitors,
  getRecentChecks,
  getRecentIncidents,
} from "../lib/api";

export default async function Home() {
  const monitors = await getMonitors();

  const monitorsWithLatestCheck = await Promise.all(
    monitors.map(async (monitor) => {

    const [checks, incidents] = await Promise.all([
      getRecentChecks(monitor.id, 30),
      getRecentIncidents(monitor.id, 3),
    ]);
      

      return {
        monitor,
        latestCheck: checks[0] ?? null,
        checks,
        incidents,
      };
    })
  );

  return (
    <main>
      <div className="dashboard-top">
        <div>
          <h1>PulseWatch</h1>

          <p className="dashboard-subtitle">
            Monitor your services in real time.
          </p>
        </div>

      <AutoRefresh intervalMs={10000} />
    </div>

      <div className="dashboard-heading">
        <h2>Monitors</h2>

        <Link href="/monitors/new" className="primary-button">
          Add Monitor
        </Link>
      </div>

      <div className="monitor-grid">
        {monitorsWithLatestCheck.map(
          ({ monitor, latestCheck , checks, incidents}) => (
            <MonitorCard
              key={monitor.id}
              monitor={monitor}
              latestCheck={latestCheck}
              checks={checks}
              incidents={incidents}
            />
          )
        )}
      </div>
    </main>
  );
}