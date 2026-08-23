export type CheckError =
  | "TIMEOUT"
  | "CONNECTION_REFUSED"
  | "DNS_ERROR"
  | "TLS_ERROR"
  | "NETWORK_ERROR";

export interface CheckResult {
  id: string;
  checkedAt: string;
  statusCode: number | null;
  latencyMs: number;
  error: CheckError | null;
}