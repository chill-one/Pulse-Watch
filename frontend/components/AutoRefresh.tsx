"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface AutoRefreshProps {
  intervalMs?: number;
}

export default function AutoRefresh({
  intervalMs = 10000,
}: AutoRefreshProps) {
  const router = useRouter();

  const [lastUpdated, setLastUpdated] = useState(
    () => Date.now()
  );

  const [secondsAgo, setSecondsAgo] = useState(0);

  useEffect(() => {
    const refreshInterval = window.setInterval(() => {
      router.refresh();
      setLastUpdated(Date.now());
    }, intervalMs);

    return () => {
      window.clearInterval(refreshInterval);
    };
  }, [router, intervalMs]);

  useEffect(() => {
    const counterInterval = window.setInterval(() => {
      setSecondsAgo(
        Math.floor(
          (Date.now() - lastUpdated) / 1000
        )
      );
    }, 1000);

    return () => {
      window.clearInterval(counterInterval);
    };
  }, [lastUpdated]);

  return (
    <div className="live-indicator">
      <span className="live-dot" />

      <span>
        Live · updated{" "}
        {secondsAgo === 0
          ? "just now"
          : `${secondsAgo}s ago`}
      </span>
    </div>
  );
}
