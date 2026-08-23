"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

interface DeleteMonitorButtonProps {
  monitorId: string;
  monitorName: string;
}

export default function DeleteMonitorButton({
  monitorId,
  monitorName,
}: DeleteMonitorButtonProps) {
  const router = useRouter();

  const [isDeleting, setIsDeleting] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  async function handleDelete() {
    const confirmed = window.confirm(
      `Delete "${monitorName}"?\n\nThis will also delete its check and incident history.`
    );

    if (!confirmed) {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/monitors/${monitorId}`,
        {
          method: "DELETE",
        }
      );

      if (!response.ok) {
        const body = await response.text();

        throw new Error(
          body ||
            `Failed to delete monitor: ${response.status}`
        );
      }

      router.push("/");
      router.refresh();
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Failed to delete monitor"
      );

      setIsDeleting(false);
    }
  }

  return (
    <>
      <button
        type="button"
        className="danger-button"
        onClick={handleDelete}
        disabled={isDeleting}
      >
        {isDeleting
          ? "Deleting..."
          : "Delete Monitor"}
      </button>

      {error && (
        <span className="delete-error">
          {error}
        </span>
      )}
    </>
  );
}