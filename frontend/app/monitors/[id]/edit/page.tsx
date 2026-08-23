import Link from "next/link";
import { notFound } from "next/navigation";

import EditMonitorForm from "../../../../components/EditMonitorForm";
import { getMonitor } from "../../../../lib/api";

export default async function EditMonitorPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  const monitor = await getMonitor(id);

  if (!monitor) {
    notFound();
  }

  return (
    <main>
      <Link
        href={`/monitors/${id}`}
        className="back-link"
      >
        ← Back to monitor
      </Link>

      <Link
        href={`/monitors/${monitor.id}/edit`}
        className="secondary-button"
        >
        Edit Monitor
    </Link>

      <p className="dashboard-subtitle">
        Update how PulseWatch monitors this service.
      </p>

      <EditMonitorForm monitor={monitor} />
    </main>
  );
}