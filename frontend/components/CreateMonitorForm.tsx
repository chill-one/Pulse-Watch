"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import router from "next/router";

export default function CreateMonitorForm() {
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [checkIntervalSeconds, setCheckIntervalSeconds] =
    useState(60);
  const [timeoutSeconds, setTimeoutSeconds] =
    useState(5);

    const router = useRouter();

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();


        setIsSubmitting(true);
        setError(null);

        try {
            const response = await fetch("/api/monitors", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                name,
                url,
                checkIntervalSeconds,
                timeoutSeconds,
            }),
            });

            if (!response.ok) {
                const body = await response.text();

                throw new Error(
                    body || `Failed to create monitor: ${response.status}`
                );
            }

            router.push("/");
            router.refresh();
        } catch (error) {
            setError(
            error instanceof Error
                ? error.message
                : "Failed to create monitor"
            );
        } finally {
            setIsSubmitting(false);
        }
    }

  return (
    <form className="monitor-form" onSubmit={handleSubmit}>
      <label>
        Name
        <input
          type="text"
          value={name}
          onChange={(event) =>
            setName(event.target.value)
          }
          placeholder="GitHub Production"
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
          placeholder="https://example.com"
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
        disabled={isSubmitting}
        >
        {isSubmitting
            ? "Creating..."
            : "Create Monitor"}
        </button>
    </form>
  );
}