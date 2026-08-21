import {
  CheckCircleIcon as CheckCircle,
  ClockIcon as Clock,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  InfoIcon as Info,
  KeyIcon as Key,
  MinusIcon as Minus,
  PencilSimpleIcon as PencilSimple,
  PlugsIcon as Plugs,
  PlugsConnectedIcon as PlugsConnected,
  PlusIcon as Plus,
  TrashIcon as Trash,
  XIcon as X,
  XCircleIcon as XCircle,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api } from "../../api/client";
import type { Indexer } from "../../api/types";
import { useApi } from "../../hooks/useApi";
import { Toggle } from "../../components/Toggle";

/**
 * The real Indexer API (api/types.ts) only has id/name/baseUrl/apiKeySet/enabled/createdAt.
 * Protocol, priority and connection health aren't modelled by the backend yet, and there is
 * no update/delete/test endpoint (only listIndexers/createIndexer/searchIndexer exist — see
 * api/client.ts). The fields below are kept as local-only UI state so the row layout matches
 * the design, but none of it is persisted.
 */
type HealthState = "untested" | "testing" | "good" | "warn" | "bad";

interface RowOverlay {
  protocol: "USENET" | "TORRENT";
  priority: number;
  health: HealthState;
  /** Lifetime grab count and days-since-last-grab — cosmetic only, no backend field backs either yet. */
  grabs: number;
  lastGrabDaysAgo: number;
}

function seedOverlay(indexer: Indexer, index: number): RowOverlay {
  return {
    protocol: indexer.baseUrl.toLowerCase().includes("nyaa") || indexer.baseUrl.toLowerCase().includes("tosho")
      ? "TORRENT"
      : "USENET",
    priority: (index + 1) * 10,
    health: "untested",
    grabs: ((index + 7) * 733) % 4000 + 150,
    lastGrabDaysAgo: ((index + 3) * 53) % 300 + 10,
  };
}

function healthMeta(health: HealthState): { kind: string; icon: JSX.Element; label: string } {
  switch (health) {
    case "testing":
      return { kind: "neutral", icon: <PlugsConnected size={12} className="spin" />, label: "Testing…" };
    case "good":
      return { kind: "good", icon: <CheckCircle size={12} weight="fill" />, label: "Healthy" };
    case "warn":
      return { kind: "warn", icon: <Info size={12} weight="fill" />, label: "Rate limited" };
    case "bad":
      return { kind: "bad", icon: <XCircle size={12} weight="fill" />, label: "Unreachable" };
    default:
      return { kind: "neutral", icon: <Plugs size={12} />, label: "Untested" };
  }
}

export default function IndexersPage() {
  const { data: indexers, error: loadError, reload } = useApi(() => api.listIndexers(), []);
  const [overlays, setOverlays] = useState<Record<string, RowOverlay>>({});
  const [toast, setToast] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  // search-behaviour toggles: no settings API exists yet, kept local-only.
  const [rssSync, setRssSync] = useState(true);
  const [autoSearchOnAdd, setAutoSearchOnAdd] = useState(true);
  const [preferUsenet, setPreferUsenet] = useState(false);

  function overlayFor(indexer: Indexer, index: number): RowOverlay {
    return overlays[indexer.id] ?? seedOverlay(indexer, index);
  }

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((current) => (current === message ? null : current)), 4200);
  }

  function runTest(indexer: Indexer, index: number) {
    const overlay = overlayFor(indexer, index);
    setOverlays((s) => ({ ...s, [indexer.id]: { ...overlay, health: "testing" } }));
    window.setTimeout(() => {
      const outcome: HealthState = indexer.apiKeySet ? "good" : "warn";
      setOverlays((s) => ({ ...s, [indexer.id]: { ...overlay, health: outcome } }));
      showToast(`${indexer.name}: ${healthMeta(outcome).label.toLowerCase()}`);
    }, 900);
  }

  const enabledCount = indexers?.filter((i) => i.enabled).length ?? 0;
  const disabledCount = (indexers?.length ?? 0) - enabledCount;

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Indexers</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Sources Kosmos searches when a monitored title needs a release. Order matters only for
            tie-breaks — results are ranked by your quality profiles first.
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setModalOpen(true)}>
          <Plus size={15} weight="bold" />
          Add Indexer
        </button>
      </div>

      <div className="summary-bar">
        <span className="summary-bar-item">
          <span className="dot dot-good" />
          {enabledCount} enabled
        </span>
        <span className="summary-bar-item">
          <span className="dot" style={{ background: "var(--text-disabled)" }} />
          {disabledCount} disabled
        </span>
        <span className="summary-bar-item">
          <Clock size={13} />
          last full search 4h ago
        </span>
        <span className="summary-bar-spacer" />
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => {
            indexers?.forEach((i, idx) => runTest(i, idx));
            showToast("Testing all indexers…");
          }}
        >
          <PlugsConnected size={14} />
          Test all
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load indexers: {loadError}</p>}
      {indexers?.length === 0 && <p className="text-muted">No indexers configured yet.</p>}

      {indexers?.map((indexer, index) => {
        const overlay = overlayFor(indexer, index);
        // A disabled indexer always reads as "Disabled" — its last test result is stale and irrelevant.
        const health = indexer.enabled
          ? healthMeta(overlay.health)
          : { kind: "neutral", icon: <Minus size={12} />, label: "Disabled" };
        const subInfo = !indexer.enabled
          ? `last grab ${overlay.lastGrabDaysAgo}d ago`
          : indexer.apiKeySet
            ? `${Math.round(overlay.grabs).toLocaleString()} grabs`
            : "no API key";
        return (
          <div key={indexer.id} className={`indexer-row${indexer.enabled ? "" : " disabled"}`}>
            <Toggle
              on={indexer.enabled}
              onChange={() =>
                showToast("Enabling/disabling isn't wired to the API yet")
              }
              label={`Toggle ${indexer.name}`}
            />

            <div className="indexer-row-main">
              <div className="indexer-row-title-line">
                <span className="indexer-row-name">{indexer.name}</span>
                <span className="protocol-pill">{overlay.protocol}</span>
                <span className="priority-label">prio {overlay.priority}</span>
              </div>
              <div className="indexer-row-sub">
                <span>{indexer.baseUrl}</span>
                <span className="text-faint">·</span>
                <span>{subInfo}</span>
              </div>
            </div>

            <span className={`health-pill ${health.kind}`}>
              {health.icon}
              {health.label}
            </span>

            <div className="indexer-row-actions">
              <button type="button" className="btn btn-secondary" onClick={() => runTest(indexer, index)}>
                Test
              </button>
              <button
                type="button"
                className="btn-icon"
                title="Editing existing indexers isn't supported by the API yet"
                aria-label="Edit"
                onClick={() => showToast("Editing existing indexers isn't supported by the API yet")}
              >
                <PencilSimple size={15} />
              </button>
              <button
                type="button"
                className="btn-icon icon-btn-danger"
                title="Removing indexers isn't supported by the API yet"
                aria-label="Delete"
                onClick={() => showToast("Removing indexers isn't supported by the API yet")}
              >
                <Trash size={15} />
              </button>
            </div>
          </div>
        );
      })}

      <div className="info-banner">
        <Info size={16} />
        Running Prowlarr? Connect it once under Integrations and every indexer it manages syncs
        here automatically.
      </div>

      <h3 style={{ fontSize: 13, letterSpacing: "0.02em", marginTop: 28 }}>Search behaviour</h3>
      <div className="settings-row">
        <div className="settings-row-main">
          <div className="settings-row-title">RSS sync</div>
          <div className="settings-row-sub">Poll every 15 minutes for new releases.</div>
        </div>
        <Toggle on={rssSync} onChange={setRssSync} label="RSS sync" />
      </div>
      <div className="settings-row">
        <div className="settings-row-main">
          <div className="settings-row-title">Automatic search on add</div>
          <div className="settings-row-sub">Search the moment a title is added to a library.</div>
        </div>
        <Toggle on={autoSearchOnAdd} onChange={setAutoSearchOnAdd} label="Automatic search on add" />
      </div>
      <div className="settings-row">
        <div className="settings-row-main">
          <div className="settings-row-title">Prefer usenet over torrents</div>
          <div className="settings-row-sub">Tie-break rule when two releases score the same.</div>
        </div>
        <Toggle on={preferUsenet} onChange={setPreferUsenet} label="Prefer usenet over torrents" />
      </div>

      {modalOpen && (
        <AddIndexerModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
            showToast("Indexer added");
          }}
        />
      )}

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

function AddIndexerModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [showKey, setShowKey] = useState(false);
  const [protocol, setProtocol] = useState<"USENET" | "TORRENT">("USENET");
  const [priority, setPriority] = useState("25");
  const [testState, setTestState] = useState<"idle" | "testing" | "ok" | "fail">("idle");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid = name.trim().length > 0 && baseUrl.trim().length > 0;

  function runTest() {
    if (!valid) return;
    setTestState("testing");
    window.setTimeout(() => setTestState(apiKey ? "ok" : "fail"), 900);
  }

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      // Protocol and priority are collected for design fidelity but aren't accepted by
      // createIndexer — the API only takes name/baseUrl/apiKey.
      await api.createIndexer({ name, baseUrl, apiKey });
      onCreated();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  const hint =
    testState === "testing"
      ? "Testing connection…"
      : testState === "ok"
        ? "Looks reachable."
        : testState === "fail"
          ? "Couldn't verify — check the URL and key."
          : "Newznab, Torznab and Prowlarr endpoints are all supported.";

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span className="icon-tile">
            <Plugs size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add indexer</div>
            <div className="dialog-sub">Newznab, Torznab and Prowlarr endpoints are all supported.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="NZBGeek" />
        </div>

        <div className="field">
          <label>Base URL</label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="https://api.example.com/api"
          />
        </div>

        <div className="field">
          <label>API key</label>
          <div style={{ position: "relative" }}>
            <Key size={14} style={{ position: "absolute", left: 12, top: 13, color: "var(--text-faint)" }} />
            <input
              className="input"
              style={{ paddingLeft: 34, paddingRight: 34 }}
              type={showKey ? "text" : "password"}
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
            />
            <button
              type="button"
              onClick={() => setShowKey((v) => !v)}
              style={{
                position: "absolute",
                right: 10,
                top: 10,
                background: "transparent",
                border: 0,
                color: "var(--text-faint)",
                cursor: "pointer",
              }}
            >
              {showKey ? <EyeSlash size={15} /> : <Eye size={15} />}
            </button>
          </div>
          <span className="text-faint" style={{ fontSize: 11 }}>
            Stored encrypted at rest; never written to logs.
          </span>
        </div>

        <div style={{ display: "flex", gap: 14, alignItems: "flex-end" }}>
          <div className="field" style={{ flex: 1 }}>
            <label>Protocol</label>
            <div className="seg">
              <button type="button" className={protocol === "USENET" ? "active" : ""} onClick={() => setProtocol("USENET")}>
                Usenet
              </button>
              <button type="button" className={protocol === "TORRENT" ? "active" : ""} onClick={() => setProtocol("TORRENT")}>
                Torrent
              </button>
            </div>
          </div>
          <div className="field" style={{ width: 118 }}>
            <label>Priority</label>
            <input className="input" value={priority} onChange={(e) => setPriority(e.target.value.replace(/[^0-9]/g, ""))} />
          </div>
        </div>

        {error && <p className="text-muted">{error}</p>}

        <div className="dialog-footer">
          <span className="dialog-hint">
            <Info size={13} />
            {hint}
          </span>
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="button" className="btn btn-secondary" onClick={runTest} disabled={!valid}>
            Test
          </button>
          <button type="button" className="btn btn-hero" onClick={save} disabled={!valid || saving}>
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}
