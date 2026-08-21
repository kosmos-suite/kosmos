import {
  ArrowsClockwiseIcon as ArrowsClockwise,
  CheckCircleIcon as CheckCircle,
  KeyIcon as Key,
  PlusIcon as Plus,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import type { JellyfinSyncResult } from "../../api/types";

export default function JellyfinPage() {
  const { data: servers, error: loadError, reload } = useApi(() => api.listJellyfinServers(), []);
  const [modalOpen, setModalOpen] = useState(false);
  const [syncingId, setSyncingId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((current) => (current === message ? null : current)), 6000);
  }

  async function runSync(serverId: string) {
    setSyncingId(serverId);
    try {
      const result: JellyfinSyncResult = await api.syncJellyfinServer(serverId);
      showToast(
        `Synced: ${result.created} movies added, ${result.linked} already-owned linked, ` +
          `${result.showsCreated} series added, ${result.episodeFilesLinked} episode files linked, ` +
          `${result.usersCreated} users added, ${result.usersUpdated} users updated.`,
      );
    } catch (e) {
      showToast(e instanceof ApiError ? `Sync failed: ${e.message}` : "Sync failed");
    } finally {
      setSyncingId(null);
    }
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Jellyfin</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Connect an already-scanned Jellyfin library — sync marks what you already have as
            available and imports its user accounts (the Jellyfin admin becomes the Kosmos admin
            automatically).
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setModalOpen(true)}>
          <Plus size={15} weight="bold" />
          Add Server
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load Jellyfin servers: {loadError}</p>}
      {servers?.length === 0 && <p className="text-muted">No Jellyfin server connected yet.</p>}

      {servers?.map((server) => (
        <div key={server.id} className={`indexer-row${server.enabled ? "" : " disabled"}`}>
          <span className="icon-tile">
            <ArrowsClockwise size={16} />
          </span>
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name">{server.name}</span>
            </div>
            <div className="indexer-row-sub">
              <span>{server.baseUrl}</span>
              <span className="text-faint">·</span>
              <span>{server.apiKeySet ? "API key set" : "no API key"}</span>
            </div>
          </div>
          <div className="indexer-row-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => runSync(server.id)}
              disabled={syncingId === server.id}
            >
              <ArrowsClockwise size={14} className={syncingId === server.id ? "spin" : ""} />
              {syncingId === server.id ? "Syncing…" : "Sync now"}
            </button>
          </div>
        </div>
      ))}

      {modalOpen && (
        <AddJellyfinServerModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
            showToast("Jellyfin server added — run a sync to pull in its library and users.");
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

function AddJellyfinServerModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [showKey, setShowKey] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid = name.trim().length > 0 && baseUrl.trim().length > 0 && apiKey.trim().length > 0;

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      await api.createJellyfinServer({ name, baseUrl, apiKey });
      onCreated();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span className="icon-tile">
            <ArrowsClockwise size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add Jellyfin server</div>
            <div className="dialog-sub">Create an API key under Dashboard → API Keys in Jellyfin.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="Home Jellyfin" />
        </div>

        <div className="field">
          <label>Base URL</label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="http://192.168.1.10:8096"
          />
        </div>

        <div className="field">
          <label>API key</label>
          <div style={{ position: "relative" }}>
            <Key size={14} style={{ position: "absolute", left: 12, top: 13, color: "var(--text-faint)" }} />
            <input
              className="input"
              style={{ paddingLeft: 34 }}
              type={showKey ? "text" : "password"}
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
            />
            <button
              type="button"
              onClick={() => setShowKey((v) => !v)}
              style={{ position: "absolute", right: 10, top: 10, background: "transparent", border: 0, color: "var(--text-faint)", cursor: "pointer" }}
            >
              {showKey ? "hide" : "show"}
            </button>
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
