import {
  CaretDownIcon as CaretDown,
  CheckCircleIcon as CheckCircle,
  ClockIcon as Clock,
  PencilSimpleIcon as PencilSimple,
  PlayIcon as Play,
  SpinnerIcon as Spinner,
  WarningIcon as Warning,
  XCircleIcon as XCircle,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api } from "../../api/client";
import type { JobRun, ScheduledJob } from "../../api/types";
import { Toggle } from "../../components/Toggle";
import { useApi } from "../../hooks/useApi";
import { relativeTime, relativeTimeUntil } from "../../utils/relativeTime";

function healthMeta(job: ScheduledJob): { kind: string; icon: JSX.Element; label: string } {
  if (job.running) return { kind: "warn", icon: <Spinner size={12} className="spin" />, label: "Running" };
  if (!job.enabled) return { kind: "neutral", icon: <Clock size={12} />, label: "Disabled" };
  if (job.lastStatus === "FAILED") return { kind: "bad", icon: <XCircle size={12} weight="fill" />, label: "Failed" };
  if (job.lastStatus === "SUCCESS") return { kind: "good", icon: <CheckCircle size={12} weight="fill" />, label: "Succeeded" };
  return { kind: "neutral", icon: <Clock size={12} />, label: "Never run" };
}

function nextRunLabel(job: ScheduledJob): string {
  if (job.running) return "running now";
  if (!job.enabled) return "not scheduled";
  if (!job.lastRunAt) return "due now";
  return relativeTimeUntil(new Date(new Date(job.lastRunAt).getTime() + job.intervalSeconds * 1000).toISOString());
}

export default function JobsPage() {
  const { data: jobs, reload } = useApi(() => api.listJobs(), []);
  const [expandedName, setExpandedName] = useState<string | null>(null);
  const [runningName, setRunningName] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((c) => (c === message ? null : c)), 3600);
  }

  async function runNow(job: ScheduledJob) {
    if (runningName) return;
    setRunningName(job.name);
    try {
      await api.runJobNow(job.name);
      showToast(`${job.displayName} ran successfully`);
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Job run failed");
    } finally {
      setRunningName(null);
      reload();
    }
  }

  async function toggleEnabled(job: ScheduledJob, enabled: boolean) {
    await api.updateJob(job.name, { enabled, intervalSeconds: job.intervalSeconds });
    reload();
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Jobs</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Kosmos runs maintenance and search tasks on a schedule — automatic search for each
            library type, and polling in-flight downloads for completion. Run any of them now
            without changing its schedule, or adjust how often it runs.
          </p>
        </div>
      </div>

      {jobs?.length === 0 && <p className="text-muted">No jobs registered yet.</p>}

      {jobs?.map((job) => (
        <JobRow
          key={job.id}
          job={job}
          expanded={expandedName === job.name}
          onToggleExpand={() => setExpandedName((n) => (n === job.name ? null : job.name))}
          onToggleEnabled={(enabled) => toggleEnabled(job, enabled)}
          onRunNow={() => runNow(job)}
          running={runningName === job.name}
          onSaved={(message) => {
            showToast(message);
            reload();
          }}
        />
      ))}

      {toast && (
        <div className="toast">
          <span className="toast-icon" style={{ background: "rgba(79,191,139,.16)", color: "var(--status-good)" }}>
            <CheckCircle size={13} weight="fill" />
          </span>
          {toast}
        </div>
      )}
    </div>
  );
}

function JobRow({
  job,
  expanded,
  onToggleExpand,
  onToggleEnabled,
  onRunNow,
  running,
  onSaved,
}: {
  job: ScheduledJob;
  expanded: boolean;
  onToggleExpand: () => void;
  onToggleEnabled: (enabled: boolean) => void;
  onRunNow: () => void;
  running: boolean;
  onSaved: (message: string) => void;
}) {
  const health = healthMeta(job);
  const { data: runs, loading: runsLoading } = useApi(
    () => (expanded ? api.listJobRuns(job.name, 10) : Promise.resolve([])),
    [expanded, job.name],
  );
  const [interval, setInterval] = useState(String(job.intervalSeconds));
  const [saving, setSaving] = useState(false);

  async function saveInterval() {
    const parsed = Number.parseInt(interval, 10);
    if (!Number.isFinite(parsed) || parsed < 10) return;
    setSaving(true);
    try {
      await api.updateJob(job.name, { enabled: job.enabled, intervalSeconds: parsed });
      onSaved(`${job.displayName}'s interval saved`);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={`job-row${job.enabled ? "" : " disabled"}`}>
      <div className="job-row-head" onClick={onToggleExpand}>
        <Toggle
          on={job.enabled}
          onChange={(next) => onToggleEnabled(next)}
          label={`Toggle ${job.displayName}`}
        />

        <div className="job-row-main">
          <div className="job-row-title-line">
            <span className="job-row-name">{job.displayName}</span>
          </div>
          <div className="job-row-sub">
            <span>every {job.intervalSeconds}s</span>
            <span>·</span>
            <span>last run {relativeTime(job.lastRunAt)}</span>
            <span>·</span>
            <span>{nextRunLabel(job)}</span>
            {job.lastMessage && (
              <>
                <span>·</span>
                <span>{job.lastMessage}</span>
              </>
            )}
          </div>
        </div>

        <span className={`health-pill ${health.kind}`}>
          {health.icon}
          {health.label}
        </span>

        <div className="job-row-actions">
          <button
            type="button"
            className="btn btn-secondary"
            disabled={running || job.running}
            onClick={(e) => {
              e.stopPropagation();
              onRunNow();
            }}
          >
            {running ? <Spinner size={14} className="spin" /> : <Play size={14} weight="fill" />}
            {running ? "Running…" : "Run Now"}
          </button>
          <CaretDown size={14} className={`job-row-chevron${expanded ? " open" : ""}`} />
        </div>
      </div>

      {expanded && (
        <div className="job-row-panel">
          <div className="job-row-interval">
            <PencilSimple size={13} className="text-faint" />
            <span className="text-faint" style={{ fontSize: 12 }}>
              Interval (seconds)
            </span>
            <input
              className="input"
              value={interval}
              onChange={(e) => setInterval(e.target.value.replace(/[^0-9]/g, ""))}
            />
            <button type="button" className="btn btn-secondary" disabled={saving} onClick={saveInterval}>
              {saving ? "Saving…" : "Save"}
            </button>
          </div>

          <div className="section-label" style={{ marginBottom: 10 }}>
            Recent runs
          </div>
          {runsLoading && <p className="text-faint" style={{ fontSize: 12 }}>Loading…</p>}
          {!runsLoading && runs?.length === 0 && (
            <p className="text-faint" style={{ fontSize: 12 }}>
              This job hasn't run yet.
            </p>
          )}
          {!runsLoading && runs && runs.length > 0 && (
            <div className="history-table">
              {runs.map((run) => (
                <JobRunRow key={run.id} run={run} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function JobRunRow({ run }: { run: JobRun }) {
  const failed = run.status === "FAILED";
  return (
    <div className="history-row" style={{ gridTemplateColumns: "auto 1fr auto auto" }}>
      <span
        className="history-row-icon"
        style={{
          background: failed ? "rgba(224,104,95,.14)" : "rgba(79,191,139,.14)",
          color: failed ? "var(--status-bad-text)" : "var(--status-good-text)",
        }}
      >
        {failed ? <Warning size={14} /> : <CheckCircle size={14} />}
      </span>
      <div style={{ minWidth: 0 }}>
        <div className="history-row-title">{run.status === "SUCCESS" ? "Succeeded" : "Failed"}</div>
        {run.message && <div className="history-row-release">{run.message}</div>}
      </div>
      <span className="text-faint" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
        {run.finishedAt && run.startedAt
          ? `${((new Date(run.finishedAt).getTime() - new Date(run.startedAt).getTime()) / 1000).toFixed(1)}s`
          : ""}
      </span>
      <span className="text-disabled" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
        {relativeTime(run.startedAt)}
      </span>
    </div>
  );
}
