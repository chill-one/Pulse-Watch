import MonitorCard from "../components/MonitorCard";
import { getMonitors, getRecentChecks } from "../lib/api";

export default async function Home() {
  const monitors = await getMonitors();

  const monitorsWithLatestCheck = await Promise.all(
    monitors.map(async (monitor) => {
      const checks = await getRecentChecks(monitor.id, 30);

      return {
        monitor,
        latestCheck: checks[0] ?? null,
        checks
      };
    })
  );

  return (
    <main>
      <h1>PulseWatch</h1>

      <p className="dashboard-subtitle">
        Monitor your services in real time.
      </p>

      <h2>Monitors</h2>

      <div className="monitor-grid">
        {monitorsWithLatestCheck.map(
          ({ monitor, latestCheck , checks}) => (
            <MonitorCard
              key={monitor.id}
              monitor={monitor}
              latestCheck={latestCheck}
              checks={checks}
            />
          )
        )}
      </div>
    </main>
  );
}