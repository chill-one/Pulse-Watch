"use client";

interface ErrorPageProps {
  error: Error & {
    digest?: string;
  };
}

export default function ErrorPage({
  error,
}: ErrorPageProps) {
  function handleRetry() {
    window.location.reload();
  }

  return (
    <main>
      <div className="error-state">
        <span className="error-icon">
          !
        </span>

        <h1>Unable to load PulseWatch</h1>

        <p>
          We couldn&apos;t retrieve monitoring data.
          The backend may be temporarily unavailable.
        </p>

        <button
          type="button"
          className="primary-button"
          onClick={handleRetry}
        >
          Try Again
        </button>

        <details>
          <summary>
            Technical details
          </summary>

          <p>{error.message}</p>
        </details>
      </div>
    </main>
  );
}
