"use client";

import {
  type FormEvent,
  useEffect,
  useState,
} from "react";
import { useRouter } from "next/navigation";

import type { Monitor } from "../types/monitor";

interface EditMonitorFormProps {
  monitor: Monitor;
}

export default function EditMonitorForm({
  monitor,
}: EditMonitorFormProps) {
  const router = useRouter();

  // Prevent form submission before React hydration finishes.
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    const hydrationFrame = window.requestAnimationFrame(() => {
      setIsHydrated(true);
    });

    return () => window.cancelAnimationFrame(hydrationFrame);
  }, []);

  const [name, setName] = useState(monitor.name);
  const [url, setUrl] = useState(monitor.url);

  const [
    checkIntervalSeconds,
    setCheckIntervalSeconds,
  ] = useState(monitor.checkIntervalSeconds);

  const [
    timeoutSeconds,
    setTimeoutSeconds,
  ] = useState(monitor.timeoutSeconds);

  const [isSubmitting, setIsSubmitting] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/monitors/${monitor.id}`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            name,
            url,
            checkIntervalSeconds,
            timeoutSeconds,
          }),
        }
      );

      if (!response.ok) {
        const body = await response.text();

        throw new Error(
          body ||
            `Failed to update monitor: ${response.status}`
        );
      }

      router.push(`/monitors/${monitor.id}`);
      router.refresh();
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Failed to update monitor"
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      className="monitor-form"
      onSubmit={handleSubmit}
    >
      <label>
        Name

        <input
          type="text"
          value={name}
          onChange={(event) =>
            setName(event.target.value)
          }
          required
        />
      </label>

      <label>
        URL

        <input
          type="url"
          value={url}
          onChange={(event) =>
            setUrl(event.target.value)
          }
          required
        />

        <span className="field-hint">
          Include http:// or https://
        </span>
      </label>

      <label>
        Check interval (seconds)

        <input
          type="number"
          min="1"
          value={checkIntervalSeconds}
          onChange={(event) =>
            setCheckIntervalSeconds(
              Number(event.target.value)
            )
          }
          required
        />
      </label>

      <label>
        Timeout (seconds)

        <input
          type="number"
          min="1"
          value={timeoutSeconds}
          onChange={(event) =>
            setTimeoutSeconds(
              Number(event.target.value)
            )
          }
          required
        />
      </label>

      {error && (
        <p className="form-error">
          {error}
        </p>
      )}

      <button
        type="submit"
        className="primary-button"
        disabled={!isHydrated || isSubmitting}
      >
        {isSubmitting
          ? "Saving..."
          : "Save Changes"}
      </button>
    </form>
  );
}
