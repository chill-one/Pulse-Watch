import MonitorCard from "../components/MonitorCard";
import { getMonitors } from "../lib/api";

export default async function Home() {
  const monitors = await getMonitors();

  return (
    <main>
      <h1>PulseWatch</h1>

      <p className="dashboard-subtitle">
        Monitor your services in real time.
      </p>

      <h2>Monitors</h2>

      <div className="monitor-grid">
        {monitors.map((monitor) => (
          <MonitorCard
            key={monitor.id}
            monitor={monitor}
          />
        ))}
      </div>
    </main>
  );
}