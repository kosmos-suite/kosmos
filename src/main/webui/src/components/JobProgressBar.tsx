import type { ScheduledJob } from "../api/types";

/** Live progress for a running {@link ScheduledJob} — see useJobPoll in the pages that use this. */
export function JobProgressBar({ job }: { job: ScheduledJob | null }) {
  if (!job || job.progressTotal === null || job.progressCurrent === null) {
    return (
      <p className="text-faint" style={{ fontSize: 12, marginTop: 10 }}>
        Starting…
      </p>
    );
  }
  const pct = job.progressTotal > 0 ? (job.progressCurrent / job.progressTotal) * 100 : 0;
  return (
    <div style={{ marginTop: 10 }}>
      <div className="progress-track" style={{ marginBottom: 6 }}>
        <div className="progress-fill" style={{ width: `${pct}%`, background: "var(--accent-gradient)" }} />
      </div>
      <div className="text-faint" style={{ fontSize: 11.5, display: "flex", justifyContent: "space-between" }}>
        <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {job.progressMessage}
        </span>
        <span style={{ flex: "none", marginLeft: 10, fontFamily: "var(--font-mono)" }}>
          {job.progressCurrent}/{job.progressTotal}
        </span>
      </div>
    </div>
  );
}
