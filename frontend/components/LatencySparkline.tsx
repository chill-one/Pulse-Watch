import type { CheckResult } from "../types/checkResult";

interface LatencySparklineProps {
  checks: CheckResult[];
}

export default function LatencySparkline({
  checks,
}: LatencySparklineProps) {
  if (checks.length < 2) {
    return <p>Not enough latency data yet.</p>;
  }

  const orderedChecks = [...checks].reverse();

  const width = 300;
  const height = 80;
  const padding = 8;

  const graphWidth = width - padding * 2;
  const graphHeight = height - padding * 2;

  const maxLatency = Math.max(
    ...orderedChecks.map((check) => check.latencyMs),
    1
  );

  const points = orderedChecks
    .map((check, index) => {
      const x =
        padding +
        (index / (orderedChecks.length - 1)) *
          graphWidth;

      const y =
        padding +
        (1 - check.latencyMs / maxLatency) *
          graphHeight;

      return `${x},${y}`;
    })
    .join(" ");

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="latency-chart"
      role="img"
      aria-label="Recent latency history"
    >
      <polyline
        points={points}
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      />
    </svg>
  );
}