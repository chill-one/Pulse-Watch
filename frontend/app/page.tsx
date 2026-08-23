import { getMonitors } from "../lib/api";
import MonitorCard from "../components/MonitorCard";

export default async function Home() {
  const monitors = await getMonitors();

  return (
    <main>
      <h1>PulseWatch</h1>
      <p>Monitoring dashboard</p>

      <h2>Monitors</h2>

        <ul>
          {monitors.map((monitor) => (
            <MonitorCard
                key={monitor.id}
                monitor={monitor}
              />
          ))}
        </ul>
    </main>
  );
}