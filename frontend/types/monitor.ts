export type MonitorStatus =
  | "PENDING"
  | "UP"
  | "DEGRADED"
  | "DOWN";

export interface Monitor {
  id: string;
  name: string;
  url: string;
  checkIntervalSeconds: number;
  timeoutSeconds: number;
  nextCheckAt: string;
  status: MonitorStatus;
  consecutiveFailureCount: number;
  createdAt: string;
}