import {
  CloudArrowDownIcon as CloudArrowDown,
  InfoIcon as Info,
  PencilSimpleIcon as PencilSimple,
  PlusIcon as Plus,
  TrashIcon as Trash,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import type { QualityDefinition } from "../../api/types";

export default function SizeLimitsPage() {
  const { data: definitions, error: loadError, reload } = useApi(() => api.listQualityDefinitions(), []);
  const [editing, setEditing] = useState<QualityDefinition | "new" | null>(null);
  const [importing, setImporting] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((current) => (current === message ? null : current)), 5000);
  }

  async function remove(id: string) {
    try {
      await api.deleteQualityDefinition(id);
      reload();
      showToast("Removed");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "Failed to remove");
    }
  }

  async function importTrashGuides() {
    if (importing) return;
    setImporting(true);
    try {
      const result = await api.importQualityDefinitionsTrashGuides();
      if (result.skipped.length > 0) {
        console.log("TRaSH-Guides size-limit import skipped:", result.skipped);
      }
      showToast(`TRaSH-Guides: ${result.created} added, ${result.updated} updated, ${result.skipped.length} skipped (see console)`);
      reload();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "Import failed");
    } finally {
      setImporting(false);
    }
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Size Limits</h2>
          <p className="text-muted" style={{ maxWidth: "62ch" }}>
            A hard floor and ceiling, in MB per minute of runtime, per resolution and source. Applied before
            custom-format scoring — a release under the floor is rejected outright as a likely fake or sample,
            regardless of how it scores.
          </p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={importTrashGuides} disabled={importing}>
          <CloudArrowDown size={14} />
          {importing ? "Importing…" : "Import from TRaSH-Guides"}
        </button>
        <button type="button" className="btn btn-hero" onClick={() => setEditing("new")}>
          <Plus size={15} weight="bold" />
          Add limit
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load size limits: {loadError}</p>}
      {definitions?.length === 0 && (
        <div className="info-banner">
          <Info size={16} />
          No size limits configured — releases pass this check by default until you add or import some.
        </div>
      )}

      {definitions && definitions.length > 0 && (
        <div style={{ marginTop: 18 }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr 120px 120px 76px",
              gap: 14,
              padding: "0 4px 9px",
              fontSize: 9.5,
              fontWeight: 600,
              letterSpacing: "0.13em",
              textTransform: "uppercase",
              color: "var(--text-ghost)",
            }}
          >
            <span>Resolution</span>
            <span>Source</span>
            <span style={{ textAlign: "right" }}>Min MB/min</span>
            <span style={{ textAlign: "right" }}>Max MB/min</span>
            <span />
          </div>
          {definitions.map((d) => (
            <div
              key={d.id}
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr 120px 120px 76px",
                gap: 14,
                alignItems: "center",
                padding: "11px 4px",
                borderBottom: "1px solid var(--border)",
                fontSize: 12.5,
              }}
            >
              <span style={{ fontWeight: 500 }}>{d.resolution}</span>
              <span className="text-secondary">{d.source}</span>
              <span style={{ textAlign: "right", fontFamily: "var(--font-mono)" }}>{d.minMbPerMinute}</span>
              <span style={{ textAlign: "right", fontFamily: "var(--font-mono)" }}>{d.maxMbPerMinute}</span>
              <span style={{ display: "flex", gap: 4, justifyContent: "flex-end" }}>
                <button type="button" className="btn btn-icon" onClick={() => setEditing(d)} aria-label="Edit">
                  <PencilSimple size={14} />
                </button>
                <button type="button" className="btn btn-icon" onClick={() => remove(d.id)} aria-label="Delete">
                  <Trash size={14} />
                </button>
              </span>
            </div>
          ))}
        </div>
      )}

      {editing && (
        <EditDefinitionModal
          definition={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            reload();
            showToast("Saved");
          }}
        />
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

function EditDefinitionModal({
  definition,
  onClose,
  onSaved,
}: {
  definition: QualityDefinition | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [resolution, setResolution] = useState(definition?.resolution ?? "1080p");
  const [source, setSource] = useState(definition?.source ?? "Blu-ray");
  const [minMbPerMinute, setMinMbPerMinute] = useState(String(definition?.minMbPerMinute ?? "50"));
  const [maxMbPerMinute, setMaxMbPerMinute] = useState(String(definition?.maxMbPerMinute ?? "2000"));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const min = Number(minMbPerMinute);
  const max = Number(maxMbPerMinute);
  const valid = resolution.trim().length > 0 && source.trim().length > 0 && Number.isFinite(min) && Number.isFinite(max) && min >= 0 && max > min;

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      const body = { resolution: resolution.trim(), source: source.trim(), minMbPerMinute: min, maxMbPerMinute: max };
      if (definition) {
        await api.updateQualityDefinition(definition.id, body);
      } else {
        await api.createQualityDefinition(body);
      }
      onSaved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span className="icon-tile">
            <PencilSimple size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">{definition ? "Edit size limit" : "Add size limit"}</div>
            <div className="dialog-sub">One limit per resolution + source combination.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div style={{ display: "flex", gap: 14 }}>
          <div className="field" style={{ flex: 1 }}>
            <label>Resolution</label>
            <input className="input" value={resolution} onChange={(e) => setResolution(e.target.value)} placeholder="1080p" />
          </div>
          <div className="field" style={{ flex: 1 }}>
            <label>Source</label>
            <input className="input" value={source} onChange={(e) => setSource(e.target.value)} placeholder="Blu-ray" />
          </div>
        </div>

        <div style={{ display: "flex", gap: 14 }}>
          <div className="field" style={{ flex: 1 }}>
            <label>Min MB/min</label>
            <input
              className="input"
              style={{ fontFamily: "var(--font-mono)" }}
              value={minMbPerMinute}
              onChange={(e) => setMinMbPerMinute(e.target.value)}
            />
          </div>
          <div className="field" style={{ flex: 1 }}>
            <label>Max MB/min</label>
            <input
              className="input"
              style={{ fontFamily: "var(--font-mono)" }}
              value={maxMbPerMinute}
              onChange={(e) => setMaxMbPerMinute(e.target.value)}
            />
          </div>
        </div>

        {error && <p className="text-muted">{error}</p>}

        <div className="dialog-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="button" className="btn btn-hero" onClick={save} disabled={!valid || saving}>
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}
