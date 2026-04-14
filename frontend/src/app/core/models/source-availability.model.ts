export interface SourceAvailability {
  source: string;
  available: boolean;
  status?: 'AVAILABLE' | 'UNAVAILABLE' | 'UNKNOWN' | null;
  message?: string | null;
  lastError: string | null;
  lastFailureAt: string | null;
  timestamp?: string | null;
}
