import type { Monitor } from "../types/monitor";
import type { CheckResult } from "../types/checkResult";

const API_URL =
  process.env.PULSEWATCH_API_URL ?? "http://localhost:8080";

export async function getMonitors(): Promise<Monitor[]> {
  const response = await fetch(`${API_URL}/monitors`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(
      `Failed to fetch monitors: ${response.status}`
    );
  }

  return response.json();
}



export async function getRecentChecks(
  monitorId: string,
  limit = 50
): Promise<CheckResult[]> {

  const response = await fetch(
    `${API_URL}/monitors/${monitorId}/checks?limit=${limit}`,
    {
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch checks for monitor ${monitorId}: ${response.status}`
    );
  }

  return response.json();
}