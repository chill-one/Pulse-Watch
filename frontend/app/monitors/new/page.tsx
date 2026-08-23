import Link from "next/link";
import CreateMonitorForm from "../../../components/CreateMonitorForm";

export default function NewMonitorPage() {
  return (
    <main>
      <Link href="/" className="back-link">
        ← Back to dashboard
      </Link>

      <h1>Add Monitor</h1>

      <p className="dashboard-subtitle">
        Add a website or service for PulseWatch to monitor.
      </p>

      <CreateMonitorForm />
    </main>
  );
}