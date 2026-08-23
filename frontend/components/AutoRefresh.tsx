"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

interface AutoRefreshProps {
  intervalMs?: number;
}

export default function AutoRefresh({
  intervalMs = 10000,
}: AutoRefreshProps) {
  const router = useRouter();

  useEffect(() => {
    const interval = window.setInterval(() => {
      router.refresh();
    }, intervalMs);

    return () => {
      window.clearInterval(interval);
    };
  }, [router, intervalMs]);

  return null;
}