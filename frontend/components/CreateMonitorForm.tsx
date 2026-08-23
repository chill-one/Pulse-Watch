"use client";

import { FormEvent, useState } from "react";

export default function CreateMonitorForm() {
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [checkIntervalSeconds, setCheckIntervalSeconds] =
    useState(60);
  const [timeoutSeconds, setTimeoutSeconds] =
    useState(5);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    console.log("SUBMIT FIRED");

    console.log({
      name,
      url,
      checkIntervalSeconds,
      timeoutSeconds,
    });
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
          placeholder="https://github.com"
          required
        />
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

      <button type="submit" className="primary-button">
        Create Monitor
      </button>
    </form>
  );
}