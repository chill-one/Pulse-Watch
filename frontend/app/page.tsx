import { getMonitors } from "../lib/api";

export default async function Home() {
  const monitors = await getMonitors();

  return (
    <main>
      <h1>PulseWatch</h1>
      <p>Monitoring dashboard</p>

      <h2>Monitors</h2>

      <ul>
        {monitors.map((monitor) => (
          <li key={monitor.id}>
            {monitor.name} — {monitor.status}
          </li>
        ))}
      </ul>
    </main>
  );
}