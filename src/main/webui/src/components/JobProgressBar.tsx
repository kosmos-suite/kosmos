import type { JobProgressEvent } from "../api/types";

/** Renders the latest {@link JobProgressEvent} from useJobProgress as a live progress bar. */
export function JobProgressBar({ event }: { event: JobProgressEvent | null }) {
  if (!event || event.kind !== "progress" || event.total === null || event.current === null) {
    return (
      <p className="text-faint" style={{ fontSize: 12, marginTop: 10 }}>
        Starting…
      </p>
    );
  }
  const pct = event.total > 0 ? (event.current / event.total) * 100 : 0;
  return (
    <div style={{ marginTop: 10 }}>
      <div className="progress-track" style={{ marginBottom: 6 }}>
        <div className="progress-fill" style={{ width: `${pct}%`, background: "var(--accent-gradient)" }} />
      </div>
      <div className="text-faint" style={{ fontSize: 11.5, display: "flex", justifyContent: "space-between" }}>
        <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{event.message}</span>
        <span style={{ flex: "none", marginLeft: 10, fontFamily: "var(--font-mono)" }}>
          {event.current}/{event.total}
        </span>
      </div>
    </div>
  );
}
