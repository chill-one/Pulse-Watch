export interface Incident {
  id: string;
  startedAt: string;
  endedAt: string | null;
  durationSeconds: number | null;
}