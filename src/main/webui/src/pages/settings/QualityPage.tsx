import {
  CheckIcon as Check,
  CloudArrowDownIcon as CloudArrowDown,
  DotsSixVerticalIcon as DotsSixVertical,
  FlaskIcon as Flask,
  FunnelIcon as Funnel,
  MinusIcon as Minus,
  PlusIcon as Plus,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import { Toggle } from "../../components/Toggle";
import type { CustomFormat, QualityProfile } from "../../api/types";

// The real ReleaseParser only extracts these fields — no "subtitles" concept exists yet
// (see parsing.ReleaseParser / RuleSpecification.FIELDS).
const FIELDS = [
  { value: "title", label: "Release title" },
  { value: "resolution", label: "Resolution" },
  { value: "source", label: "Source" },
  { value: "videoCodec", label: "Video codec" },
  { value: "audioCodec", label: "Audio codec" },
  { value: "edition", label: "Edition" },
  { value: "releaseGroup", label: "Release group" },
  { value: "seasonPack", label: "Season pack" },
] as const;
const OPS = ["contains", "is", "starts with"] as const;
const OP_WORDS: Record<(typeof OPS)[number], string> = {
  contains: "contains",
  is: "is exactly",
  "starts with": "starts with",
};

function formatScore(score: number): string {
  return (score > 0 ? "+" : "") + score;
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** Builds one RuleSpecification (parsing.RuleSpecification on the backend) from the plain-English builder. */
function buildRule(field: string, op: (typeof OPS)[number], value: string): string {
  const spec =
    op === "is"
      ? { field, matchType: "equals", value, negate: false, required: true }
      : op === "starts with"
        ? { field, matchType: "regex", value: `^${escapeRegex(value)}`, negate: false, required: true }
        : { field, matchType: "regex", value: escapeRegex(value), negate: false, required: true };
  return JSON.stringify([spec]);
}

export default function QualityPage() {
  const { data: profiles, error: loadError, reload: reloadProfiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: allFormats, reload: reloadFormats } = useApi(() => api.listCustomFormats(), []);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [order, setOrder] = useState<string[]>([]);
  const [drag, setDrag] = useState<number | null>(null);
  const [dragOver, setDragOver] = useState<number | null>(null);
  const [builderOpen, setBuilderOpen] = useState(false);
  const [builder, setBuilder] = useState({
    name: "",
    field: "title" as (typeof FIELDS)[number]["value"],
    op: "contains" as (typeof OPS)[number],
    value: "",
    score: 20,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedId && profiles && profiles.length > 0) {
      setSelectedId(profiles[0].id);
    }
  }, [profiles, selectedId]);

  const profile = profiles?.find((p) => p.id === selectedId) ?? null;

  useEffect(() => {
    setOrder(profile ? profile.customFormats.map((f) => f.id) : []);
  }, [profile?.id, profile?.customFormats.length]);

  const orderedFormats = useMemo(() => {
    if (!profile) return [];
    const byId = new Map(profile.customFormats.map((f) => [f.id, f]));
    return order.map((id) => byId.get(id)).filter((f): f is CustomFormat => !!f);
  }, [profile, order]);

  const total = useMemo(() => orderedFormats.reduce((sum, f) => sum + f.score, 0), [orderedFormats]);
  const pass = profile ? total >= profile.cutoffScore : false;

  const visibleFormats = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return orderedFormats;
    return orderedFormats.filter((f) => f.name.toLowerCase().includes(q));
  }, [orderedFormats, query]);

  async function setCutoffScore(v: number) {
    if (!profile) return;
    const clamped = Math.max(0, Math.min(400, Math.round(v / 5) * 5));
    try {
      await api.updateQualityProfile(profile.id, {
        name: profile.name,
        cutoffScore: clamped,
        customFormatIds: profile.customFormats.map((f) => f.id),
      });
      reloadProfiles();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function toggleFormatInProfile(formatId: string) {
    if (!profile) return;
    const ids = new Set(profile.customFormats.map((f) => f.id));
    if (ids.has(formatId)) {
      ids.delete(formatId);
    } else {
      ids.add(formatId);
    }
    try {
      await api.updateQualityProfile(profile.id, {
        name: profile.name,
        cutoffScore: profile.cutoffScore,
        customFormatIds: [...ids],
      });
      reloadProfiles();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  function moveFormat(from: number, to: number) {
    setOrder((current) => {
      const next = current.slice();
      const [row] = next.splice(from, 1);
      next.splice(to, 0, row);
      return next;
    });
  }

  async function addProfile() {
    setSaving(true);
    setError(null);
    try {
      const created = await api.createQualityProfile({ name: "New profile", cutoffScore: 0, customFormatIds: [] });
      await reloadProfiles();
      setSelectedId(created.id);
      setQuery("");
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  const bValid = builder.name.trim().length > 1 && builder.value.trim().length > 0;

  async function saveFormat() {
    if (!bValid || !profile) return;
    setSaving(true);
    setError(null);
    try {
      const created = await api.createCustomFormat({
        name: builder.name.trim(),
        score: builder.score,
        rule: buildRule(builder.field, builder.op, builder.value.trim()),
      });
      await api.updateQualityProfile(profile.id, {
        name: profile.name,
        cutoffScore: profile.cutoffScore,
        customFormatIds: [...profile.customFormats.map((f) => f.id), created.id],
      });
      await Promise.all([reloadProfiles(), reloadFormats()]);
      setBuilder({ name: "", field: "title", op: "contains", value: "", score: 20 });
      setBuilderOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  async function importTrashGuides() {
    if (importing) return;
    setImporting(true);
    setError(null);
    try {
      const result = await api.importTrashGuides();
      if (result.skipped.length > 0) {
        console.log("TRaSH-Guides import skipped:", result.skipped);
      }
      setToast(
        `TRaSH-Guides: ${result.created} added, ${result.updated} updated, ${result.skipped.length} skipped (see console)`,
      );
      window.setTimeout(() => setToast((current) => (current === null ? current : null)), 6000);
      await reloadFormats();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "TRaSH-Guides import failed");
    } finally {
      setImporting(false);
    }
  }

  if (loadError) {
    return <p className="text-muted">Failed to load quality profiles: {loadError}</p>;
  }
  if (!profiles || !allFormats) {
    return null;
  }

  return (
    <div>
      <div className="quality-layout">
        <div className="quality-profile-list">
          {profiles.map((p) => {
            const active = p.id === selectedId;
            return (
              <button
                key={p.id}
                className={active ? "active" : ""}
                onClick={() => {
                  setSelectedId(p.id);
                  setQuery("");
                }}
                style={{ display: "flex", flexDirection: "column", alignItems: "stretch", gap: 4 }}
              >
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ flex: 1 }}>{p.name}</span>
                  <span
                    style={{ fontFamily: "var(--font-mono)", fontSize: 10.5, color: active ? "var(--accent-tint)" : "var(--text-disabled)" }}
                  >
                    ≥{p.cutoffScore}
                  </span>
                </span>
                <span style={{ fontSize: 10.5, color: "var(--text-disabled)", fontWeight: 400 }}>
                  {p.customFormats.length} formats
                </span>
              </button>
            );
          })}
          <button type="button" className="new-profile" onClick={addProfile} disabled={saving}>
            <Plus size={14} />
            New profile
          </button>
        </div>

        {!profile ? (
          <div className="quality-detail">
            <p className="text-muted">No quality profile yet — create one to start scoring releases.</p>
          </div>
        ) : (
          <div className="quality-detail">
            <div className="page-header" style={{ padding: "0 0 20px" }}>
              <div style={{ flex: 1, minWidth: 280 }}>
                <h2 style={{ margin: 0 }}>{profile.name}</h2>
              </div>
            </div>

            {error && <p className="text-muted">{error}</p>}

            <div className="score-card">
              <div className="score-card-head">
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, fontSize: 14, marginBottom: 5 }}>Minimum score to grab</div>
                  <div className="text-muted" style={{ fontSize: 12, maxWidth: "52ch" }}>
                    A release must total at least this many points from the formats below.
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 8, flex: "none" }}>
                  <input
                    className="score-input"
                    value={profile.cutoffScore}
                    onChange={(e) => setCutoffScore(parseInt(e.target.value.replace(/[^0-9]/g, ""), 10) || 0)}
                  />
                  <span className="text-faint" style={{ fontSize: 11.5, width: 44 }}>
                    points
                  </span>
                </div>
              </div>

              <div
                className="score-slider"
                onPointerDown={(e) => {
                  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
                  const r = e.currentTarget.getBoundingClientRect();
                  setCutoffScore(((e.clientX - r.left) / r.width) * 400);
                }}
                onPointerMove={(e) => {
                  if (e.buttons !== 1) return;
                  const r = e.currentTarget.getBoundingClientRect();
                  setCutoffScore(((e.clientX - r.left) / r.width) * 400);
                }}
              >
                <div className="score-slider-track" />
                <div className="score-slider-fill" style={{ width: `${(profile.cutoffScore / 400) * 100}%` }} />
                <div className="score-slider-knob" style={{ left: `${(profile.cutoffScore / 400) * 100}%` }} />
                <div
                  className="score-slider-mark"
                  style={{
                    left: `${Math.min(100, (total / 400) * 100)}%`,
                    background: pass ? "rgba(79,191,139,.9)" : "rgba(224,104,95,.9)",
                  }}
                />
              </div>
              <div className="score-scale">
                <span>0</span>
                <span>100</span>
                <span>200</span>
                <span>300</span>
                <span>400</span>
              </div>
              <div className="text-faint" style={{ fontSize: 11.5, marginTop: 10 }}>
                This profile's formats currently total {total} pts against real releases you score with it.
              </div>
            </div>

            <div className="format-table">
              <div className="format-table-toolbar">
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, fontSize: 13.5, marginBottom: 4 }}>Custom formats</div>
                  <div className="text-faint" style={{ fontSize: 11.5 }}>
                    {orderedFormats.length} in this profile · {allFormats.length} total across all profiles
                  </div>
                </div>
                <div className="registry-search" style={{ width: 212, flex: "none" }}>
                  <span className="text-faint">
                    <Funnel size={13} />
                  </span>
                  <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Filter formats…" />
                  {query && (
                    <span style={{ cursor: "pointer", color: "var(--text-faint)" }} onClick={() => setQuery("")}>
                      <X size={12} />
                    </span>
                  )}
                </div>
                <button type="button" className="btn btn-secondary" onClick={importTrashGuides} disabled={importing}>
                  <CloudArrowDown size={14} />
                  {importing ? "Importing…" : "Import from TRaSH-Guides"}
                </button>
                <button type="button" className="btn btn-hero" onClick={() => setBuilderOpen(true)}>
                  <Plus size={14} />
                  New Custom Format
                </button>
              </div>

              <div className="format-table-head">
                <span />
                <span>Format</span>
                <span style={{ textAlign: "right" }}>Score</span>
                <span style={{ textAlign: "right" }}>In profile</span>
              </div>

              {visibleFormats.map((f) => {
                const index = orderedFormats.indexOf(f);
                return (
                  <div
                    key={f.id}
                    draggable
                    onDragStart={() => setDrag(index)}
                    onDragEnter={() => setDragOver(index)}
                    onDragOver={(e) => e.preventDefault()}
                    onDragEnd={() => {
                      setDrag(null);
                      setDragOver(null);
                    }}
                    onDrop={() => {
                      if (drag !== null && drag !== index) moveFormat(drag, index);
                    }}
                    className={`format-drag-row${drag === index ? " dragging" : ""}${dragOver === index && drag !== null && drag !== index ? " drag-over" : ""}`}
                  >
                    <span className="format-drag-handle">
                      <DotsSixVertical size={16} />
                    </span>
                    <div style={{ minWidth: 0, display: "flex", alignItems: "center", gap: 7 }}>
                      <div className="format-name">{f.name}</div>
                      {f.trashId && (
                        <span
                          className="text-faint"
                          style={{ fontSize: 9.5, letterSpacing: "0.06em", textTransform: "uppercase", border: "1px solid var(--border)", borderRadius: 4, padding: "1px 5px", flex: "none" }}
                          title="Imported from TRaSH-Guides"
                        >
                          TRaSH
                        </span>
                      )}
                    </div>
                    <span
                      style={{ fontFamily: "var(--font-mono)", fontWeight: 500, fontSize: 12.5, textAlign: "right", color: f.score > 0 ? "var(--status-good)" : "var(--status-bad-text)" }}
                    >
                      {formatScore(f.score)}
                    </span>
                    <div style={{ justifySelf: "end" }}>
                      <Toggle on={true} onChange={() => toggleFormatInProfile(f.id)} label={`Remove ${f.name} from this profile`} />
                    </div>
                  </div>
                );
              })}

              {visibleFormats.length === 0 && orderedFormats.length > 0 && (
                <div style={{ padding: "34px 22px", textAlign: "center" }}>
                  <div style={{ fontWeight: 500, fontSize: 13.5, marginBottom: 6 }}>No format matches "{query}"</div>
                </div>
              )}

              {orderedFormats.length === 0 && (
                <div style={{ padding: "34px 22px", textAlign: "center" }}>
                  <div className="text-muted" style={{ fontSize: 12 }}>
                    No custom formats in this profile yet.
                  </div>
                </div>
              )}

              {allFormats.some((f) => !profile.customFormats.some((pf) => pf.id === f.id)) && (
                <div className="format-table-footer" style={{ flexWrap: "wrap", gap: 6 }}>
                  <span className="text-faint" style={{ fontSize: 11.5 }}>
                    Not in this profile:
                  </span>
                  {allFormats
                    .filter((f) => !profile.customFormats.some((pf) => pf.id === f.id))
                    .map((f) => (
                      <button key={f.id} type="button" className="chip-filter" onClick={() => toggleFormatInProfile(f.id)}>
                        <Plus size={11} />
                        {f.name}
                      </button>
                    ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {builderOpen && (
        <div className="dialog-backdrop" onClick={() => setBuilderOpen(false)}>
          <div className="dialog" style={{ width: 596 }} onClick={(e) => e.stopPropagation()}>
            <div className="dialog-header">
              <span className="icon-tile">
                <Funnel size={16} />
              </span>
              <div className="dialog-header-body">
                <div className="dialog-title">New custom format</div>
                <div className="dialog-sub">Build the rule in plain terms — no regex needed.</div>
              </div>
              <button type="button" className="dialog-close" onClick={() => setBuilderOpen(false)}>
                <X size={15} />
              </button>
            </div>

            <div className="field">
              <label>Format name</label>
              <input
                className="input"
                value={builder.name}
                onChange={(e) => setBuilder((b) => ({ ...b, name: e.target.value }))}
                placeholder="e.g. Remux"
              />
            </div>

            <div className="field">
              <label>Condition</label>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 9, marginBottom: 9 }}>
                <div>
                  <div style={{ fontSize: 10, letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--text-ghost)", marginBottom: 6 }}>
                    When
                  </div>
                  <div className="pill-picker">
                    {FIELDS.map((f) => (
                      <button
                        key={f.value}
                        type="button"
                        className={builder.field === f.value ? "active" : ""}
                        onClick={() => setBuilder((b) => ({ ...b, field: f.value }))}
                      >
                        {f.label}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 10, letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--text-ghost)", marginBottom: 6 }}>
                    Test
                  </div>
                  <div className="pill-picker">
                    {OPS.map((op) => (
                      <button key={op} type="button" className={builder.op === op ? "active" : ""} onClick={() => setBuilder((b) => ({ ...b, op }))}>
                        {op}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
              <input
                className="input"
                style={{ fontFamily: "var(--font-mono)" }}
                value={builder.value}
                onChange={(e) => setBuilder((b) => ({ ...b, value: e.target.value }))}
                placeholder="value — e.g. BluRay Remux, H.265"
              />
            </div>

            <div style={{ display: "flex", alignItems: "flex-end", gap: 14 }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 11.5, color: "var(--text-secondary)", marginBottom: 7 }}>
                  Score when matched
                </label>
                <div className="stepper">
                  <span className="stepper-btn" onClick={() => setBuilder((b) => ({ ...b, score: Math.max(-1000, b.score - 5) }))}>
                    <Minus size={14} />
                  </span>
                  <div className="stepper-value" style={{ color: builder.score > 0 ? "var(--status-good)" : builder.score === 0 ? "var(--text-muted)" : "var(--status-bad-text)" }}>
                    {formatScore(builder.score)}
                  </div>
                  <span className="stepper-btn" onClick={() => setBuilder((b) => ({ ...b, score: Math.min(1000, b.score + 5) }))}>
                    <Plus size={14} />
                  </span>
                </div>
              </div>
              <div className="reads-as" style={{ flex: 1 }}>
                <div className="reads-as-label">Reads as</div>
                <div className="reads-as-value">
                  Give {formatScore(builder.score)} points when {FIELDS.find((f) => f.value === builder.field)?.label.toLowerCase()}{" "}
                  {OP_WORDS[builder.op]} "{builder.value.trim() || "…"}".
                </div>
              </div>
            </div>

            <div className="dialog-footer">
              <span className="dialog-hint">
                <Flask size={13} />
                Scored the same way real indexer searches are — no live preview against past releases yet.
              </span>
              <span className="btn btn-secondary" onClick={() => setBuilderOpen(false)} style={{ cursor: "pointer" }}>
                Cancel
              </span>
              <button type="button" className="btn btn-hero" disabled={!bValid || saving} onClick={saveFormat}>
                {saving ? "Saving…" : "Add format"}
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="toast">
          <span className="toast-icon">
            <CloudArrowDown size={12} />
          </span>
          <span style={{ fontSize: 12.5 }}>{toast}</span>
        </div>
      )}
    </div>
  );
}
