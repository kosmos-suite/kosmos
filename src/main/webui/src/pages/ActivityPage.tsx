import {
  ArrowDownIcon as ArrowDown,
  ArrowFatLineUpIcon as ArrowFatLineUp,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  ClockCounterClockwiseIcon as ClockCounterClockwise,
  DotsThreeIcon as DotsThree,
  EyeIcon as Eye,
  MagnifyingGlassIcon as MagnifyingGlass,
  PauseIcon as Pause,
  PlayIcon as Play,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import {
  activeDownloadSources,
  historyFilterKind,
  historyFilters,
  mockHistory,
  type HistoryEventKind,
  type HistoryFilter,
} from "../mocks/mockActivity";
import { tonalGradient } from "../utils/tonalGradient";

function relativeTime(iso: string | null): string {
  if (!iso) return "never run";
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "just now";
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  return `${Math.floor(seconds / 3600)}h ago`;
}

const EVENT_META: Record<HistoryEventKind, { icon: typeof Check; bgClass: string; fgVar: string }> = {
  done: { icon: Check, bgClass: "", fgVar: "#7FD6AC" },
  upgrade: { icon: ArrowFatLineUp, bgClass: "", fgVar: "#C3BAFB" },
  grab: { icon: ArrowDown, bgClass: "", fgVar: "#EBC182" },
  fail: { icon: X, bgClass: "", fgVar: "#EE9891" },
};
const EVENT_BG: Record<HistoryEventKind, string> = {
  done: "rgba(79,191,139,.14)",
  upgrade: "rgba(145,132,217,.16)",
  grab: "rgba(224,169,74,.14)",
  fail: "rgba(224,104,95,.14)",
};

function fmtGb(gb: number): string {
  return `${gb.toFixed(1)} GB`;
}

export default function ActivityPage() {
  const { data: jobs } = useApi(() => api.listJobs(), []);
  const [tick, setTick] = useState(0);
  const [hovered, setHovered] = useState<number | null>(null);
  const [previewEmpty, setPreviewEmpty] = useState(false);
  const [histFilter, setHistFilter] = useState<HistoryFilter>("All");

  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, []);

  const isEmpty = previewEmpty;

  const active = useMemo(
    () =>
      activeDownloadSources.map((d, i) => {
        const paused = d.state === "paused";
        const raw = paused ? d.base : d.base + d.rate * tick;
        const pct = Math.min(99.4, raw);
        const secsLeft = paused ? 0 : Math.max(6, Math.round((100 - pct) / d.rate));
        const eta = paused ? "paused" : secsLeft > 90 ? `${Math.round(secsLeft / 60)}m left` : `${secsLeft}s left`;
        return {
          ...d,
          index: i,
          pct,
          eta,
          doneGb: fmtGb((d.sizeGb * pct) / 100),
          sizeLabel: fmtGb(d.sizeGb),
          speedLabel: paused ? "— MB/s" : `${d.speedMbps.toFixed(1)} MB/s`,
        };
      }),
    [tick],
  );

  const totalSpeed = activeDownloadSources.reduce((a, d) => a + (d.state === "paused" ? 0 : d.speedMbps), 0);
  const downloadingCount = activeDownloadSources.filter((d) => d.state === "downloading").length;
  const pausedCount = activeDownloadSources.length - downloadingCount;

  const groups = useMemo(
    () =>
      mockHistory
        .map((g) => ({
          label: g.label,
          rows: g.rows.filter((r) => !historyFilterKind[histFilter] || r.kind === historyFilterKind[histFilter]),
        }))
        .filter((g) => g.rows.length > 0),
    [histFilter],
  );

  const headline = isEmpty
    ? "queue clear · 34 events in history"
    : `${downloadingCount} downloading · ${pausedCount} paused · ${totalSpeed.toFixed(1)} MB/s`;

  const stats = isEmpty
    ? [
        { label: "Queue", value: "Empty", dotClass: "" },
        { label: "Speed", value: "0 MB/s", dotClass: "" },
        { label: "Imported today", value: "6 files", dotClass: "" },
        { label: "Missing", value: "7 titles", dotClass: "" },
      ]
    : [
        { label: "Downloading", value: `${downloadingCount} of ${activeDownloadSources.length}`, dotClass: "dot-warn pulsing" },
        { label: "Combined speed", value: `${totalSpeed.toFixed(1)} MB/s`, dotClass: "dot pulsing", dotBg: "var(--accent-2)" },
        { label: "Imported today", value: "6 files", dotClass: "dot-good" },
        { label: "Failed today", value: "2 grabs", dotClass: "dot-bad" },
      ];

  return (
    <div className="page with-top-padding">
      <div className="page-header" style={{ alignItems: "flex-start", justifyContent: "space-between", marginBottom: 16 }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 16 }}>
          <h1>Activity</h1>
          <span className="text-faint" style={{ fontSize: 12.5 }}>
            {headline}
          </span>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button type="button" className="btn btn-ghost" onClick={() => setPreviewEmpty((v) => !v)}>
            <Eye size={13} />
            {isEmpty ? "Show active queue" : "Preview empty queue"}
          </button>
          <button type="button" className="btn btn-secondary">
            <Pause size={14} />
            Pause all
          </button>
        </div>
      </div>

      <div className="stat-grid" style={{ marginBottom: isEmpty ? 0 : 28 }}>
        {stats.map((s) => (
          <div className="stat-card" key={s.label}>
            <div className="stat-card-label">
              <span className={`dot ${s.dotClass}`} style={s.dotBg ? { background: s.dotBg } : undefined} />
              {s.label.toUpperCase()}
            </div>
            <div className="stat-card-value">{s.value}</div>
          </div>
        ))}
      </div>

      {/* Real — GET /jobs. The queue/history below are still mock: no live per-torrent progress
          feed or grab-history endpoint exists yet, only this background-task list does. */}
      {jobs && jobs.length > 0 && (
        <div style={{ marginTop: 28 }}>
          <h2 style={{ margin: "0 0 14px", fontSize: 17, fontWeight: 500, letterSpacing: "-0.02em" }}>Background jobs</h2>
          <div className="history-table">
            {jobs.map((job) => (
              <div className="history-row" key={job.id} style={{ gridTemplateColumns: "auto 1fr auto auto" }}>
                <span
                  className="history-row-icon"
                  style={{
                    background: job.lastStatus === "FAILED" ? "rgba(224,104,95,.14)" : "rgba(79,191,139,.14)",
                    color: job.lastStatus === "FAILED" ? "#EE9891" : "#7FD6AC",
                  }}
                >
                  <Check size={14} />
                </span>
                <div style={{ minWidth: 0 }}>
                  <div className="history-row-title">{job.name}</div>
                  <div className="history-row-release">
                    every {job.intervalSeconds}s{job.lastMessage ? ` · ${job.lastMessage}` : ""}
                  </div>
                </div>
                <span className="text-faint" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                  {job.lastStatus ?? "pending"}
                </span>
                <span className="text-disabled" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                  {relativeTime(job.lastRunAt)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!isEmpty && (
        <div style={{ marginTop: 28, marginBottom: 8 }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 11, marginBottom: 14 }}>
            <h2 style={{ margin: 0, fontSize: 17, fontWeight: 500, letterSpacing: "-0.02em" }}>Downloading now</h2>
            <span className="text-faint" style={{ fontSize: 11.5 }}>
              {activeDownloadSources.length} in queue · {totalSpeed.toFixed(1)} MB/s combined
            </span>
          </div>

          {active.map((d) => {
            const paused = d.state === "paused";
            const on = hovered === d.index;
            return (
              <div
                key={d.title}
                className="download-row"
                style={on ? { background: "var(--bg-elevated)", borderColor: "var(--border)" } : undefined}
                onMouseEnter={() => setHovered(d.index)}
                onMouseLeave={() => setHovered(null)}
              >
                <div
                  style={{
                    width: 46,
                    aspectRatio: "2 / 3",
                    borderRadius: 8,
                    overflow: "hidden",
                    flex: "none",
                    border: "1px solid var(--border-subtle)",
                    background: tonalGradient(d.index + 1),
                  }}
                />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 5 }}>
                    <span style={{ fontSize: 14.5, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                      {d.title}
                    </span>
                    <span className="tag-outline" style={{ padding: "3px 7px", fontSize: 10.5, fontFamily: "var(--font-mono)", flex: "none" }}>
                      {d.tag}
                    </span>
                    <span
                      style={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 5,
                        flex: "none",
                        fontSize: 10,
                        fontWeight: 600,
                        letterSpacing: "0.08em",
                        textTransform: "uppercase",
                        color: paused ? "var(--text-muted)" : "#EBC182",
                      }}
                    >
                      <span className={`dot${paused ? "" : " pulsing"}`} style={{ background: paused ? "var(--text-disabled)" : "#E0A94A" }} />
                      {paused ? "Paused" : "Downloading"}
                    </span>
                  </div>
                  <div
                    style={{
                      fontFamily: "var(--font-mono)",
                      fontSize: 11,
                      color: "var(--text-faint)",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                      marginBottom: 9,
                    }}
                  >
                    {d.release}
                  </div>
                  <div className="progress-track" style={{ marginBottom: 8 }}>
                    <div
                      className="progress-fill"
                      style={{
                        width: `${d.pct}%`,
                        background: paused ? "rgba(233,233,237,.28)" : "var(--accent-gradient)",
                        transition: "width 900ms linear",
                      }}
                    />
                  </div>
                  <div className="download-stat-line">
                    <span style={{ color: paused ? "var(--text-secondary)" : "var(--accent-2)", fontWeight: 500 }}>{d.pct.toFixed(1)}%</span>
                    <span>
                      {d.doneGb} of {d.sizeLabel}
                    </span>
                    <span className="text-faint">{d.speedLabel}</span>
                    <span className="text-faint">{d.eta}</span>
                    <span className="text-disabled">{d.seeds}</span>
                    <span className="text-disabled">{d.indexer}</span>
                  </div>
                </div>
                <div className="download-row-actions" style={{ opacity: on ? 1 : 0.5, transition: "opacity 160ms" }}>
                  <button type="button" className="btn-icon">
                    {paused ? <Play size={15} weight="fill" /> : <Pause size={15} />}
                  </button>
                  <button type="button" className="btn-icon">
                    <X size={15} />
                  </button>
                  <button type="button" className="btn-icon">
                    <DotsThree size={16} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isEmpty && (
        <div className="empty-state">
          <div className="empty-state-inner">
            <div
              style={{
                position: "relative",
                width: 140,
                height: 140,
                margin: "0 auto 30px",
                display: "grid",
                placeItems: "center",
              }}
            >
              <span style={{ position: "absolute", inset: 0, borderRadius: "50%", border: "1px dashed rgba(233,233,237,.1)" }} />
              <span style={{ position: "absolute", inset: 22, borderRadius: "50%", border: "1px dashed rgba(233,233,237,.09)" }} />
              <span
                style={{
                  position: "absolute",
                  inset: 44,
                  borderRadius: "50%",
                  border: "1px solid rgba(145,132,217,.24)",
                  background: "rgba(145,132,217,.07)",
                }}
              />
              <ArrowDown size={26} color="#B5ABFC" style={{ position: "relative" }} />
            </div>
            <div className="empty-state-title">Nothing downloading right now</div>
            <p className="empty-state-body">
              The queue is clear. Kosmos keeps watching your monitored titles and will start a grab the moment a release clears your
              quality profile.
            </p>
            <div className="empty-state-actions">
              <button type="button" className="btn btn-hero">
                <MagnifyingGlass size={15} />
                Search all missing (7)
              </button>
              <button type="button" className="btn btn-secondary">
                <ClockCounterClockwise size={15} />
                Jump to history
              </button>
            </div>
          </div>
        </div>
      )}

      <div style={{ marginTop: isEmpty ? 8 : 34 }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 17, fontWeight: 500, letterSpacing: "-0.02em" }}>History</h2>
          <span className="text-faint" style={{ fontSize: 11.5 }}>
            last 48 hours · 34 events
          </span>
          <div style={{ flex: 1 }} />
          <div className="filter-tabs">
            {historyFilters.map((f) => (
              <button key={f} className={histFilter === f ? "active" : ""} onClick={() => setHistFilter(f)}>
                {f}
              </button>
            ))}
          </div>
        </div>

        {groups.map((g) => (
          <div className="history-group" key={g.label}>
            <div className="history-group-label">{g.label}</div>
            <div className="history-table">
              {g.rows.map((h) => {
                const meta = EVENT_META[h.kind];
                const Icon = meta.icon;
                return (
                  <div className="history-row" key={`${g.label}-${h.title}-${h.time}`}>
                    <span className="history-row-icon" style={{ background: EVENT_BG[h.kind], color: meta.fgVar }}>
                      <Icon size={14} />
                    </span>
                    <div style={{ minWidth: 0 }}>
                      <div className="history-row-title" style={{ color: h.kind === "fail" ? "var(--text-secondary)" : "var(--text-primary)" }}>
                        {h.title}
                      </div>
                      <div className="history-row-release">{h.release}</div>
                    </div>
                    <span style={{ fontFamily: "var(--font-mono)", fontSize: 11.5, color: meta.fgVar }}>{h.event}</span>
                    <span className="text-faint" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                      {h.size}
                    </span>
                    <span className="text-disabled" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                      {h.time}
                    </span>
                    <button type="button" className="btn-icon" style={{ width: 26, height: 26 }}>
                      <DotsThree size={14} />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        <div style={{ display: "flex", justifyContent: "center", paddingTop: 4 }}>
          <button type="button" className="btn btn-ghost">
            Load older events
            <CaretDown size={12} />
          </button>
        </div>
      </div>
    </div>
  );
}
