import type { Monitor } from "../types/monitor";
import type { CheckResult } from "../types/checkResult";
import type { Incident } from "../types/incident";

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

export async function getRecentIncidents(
  monitorId: string,
  limit = 10
): Promise<Incident[]> {
  const response = await fetch(
    `${API_URL}/monitors/${monitorId}/incidents?limit=${limit}`,
    {
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch incidents for monitor ${monitorId}: ${response.status}`
    );
  }

  return response.json();
}


export async function getMonitor( monitorId: string ): Promise<Monitor | null> {
  const response = await fetch(
      `${API_URL}/monitors/${monitorId}`,
      {
        cache: "no-store",
      }
    );

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      throw new Error(
        `Failed to fetch monitor ${monitorId}: ${response.status}`
      );
    }

    return response.json();
}