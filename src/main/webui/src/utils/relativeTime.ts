/** "3m ago", "2h ago", "just now" — or the given fallback for a null timestamp. */
export function relativeTime(iso: string | null, fallback = "never run"): string {
  if (!iso) return fallback;
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "just now";
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

/** "in 3m", "in 2h", "due now" — the flip side of {@link relativeTime}, for a next-run estimate. */
export function relativeTimeUntil(iso: string): string {
  const seconds = Math.floor((new Date(iso).getTime() - Date.now()) / 1000);
  if (seconds <= 0) return "due now";
  if (seconds < 60) return "in less than a minute";
  if (seconds < 3600) return `in ${Math.floor(seconds / 60)}m`;
  if (seconds < 86400) return `in ${Math.floor(seconds / 3600)}h`;
  return `in ${Math.floor(seconds / 86400)}d`;
}
