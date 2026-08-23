import type { Monitor } from "../types/monitor";

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