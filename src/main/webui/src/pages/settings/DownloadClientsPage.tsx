import {
  CheckCircleIcon as CheckCircle,
  DownloadIcon as Download,
  KeyIcon as Key,
  PlusIcon as Plus,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";

export default function DownloadClientsPage() {
  const { data: clients, error: loadError, reload } = useApi(() => api.listDownloadClients(), []);
  const [modalOpen, setModalOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((current) => (current === message ? null : current)), 4200);
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Download Clients</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Where Kosmos sends grabbed releases. qBittorrent, Transmission, Deluge, SABnzbd, and NZBGet are all
            supported.
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setModalOpen(true)}>
          <Plus size={15} weight="bold" />
          Add Download Client
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load download clients: {loadError}</p>}
      {clients?.length === 0 && <p className="text-muted">No download clients configured yet.</p>}

      {clients?.map((client) => (
        <div key={client.id} className={`indexer-row${client.enabled ? "" : " disabled"}`}>
          <span className="icon-tile">
            <Download size={16} />
          </span>
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name">{client.name}</span>
              <span className="protocol-pill">{client.type}</span>
            </div>
            <div className="indexer-row-sub">
              <span>{client.baseUrl}</span>
              <span className="text-faint">·</span>
              <span>{client.category ? `category: ${client.category}` : "no category"}</span>
              <span className="text-faint">·</span>
              <span>{client.passwordSet ? "credentials set" : "no password"}</span>
            </div>
          </div>
        </div>
      ))}

      {modalOpen && (
        <AddDownloadClientModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
            showToast("Download client added");
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

const CLIENT_TYPES = [
  { value: "QBITTORRENT" as const, label: "qBittorrent", hint: "Web API — enable it under Tools → Options → Web UI.", defaultPort: "8080" },
  { value: "TRANSMISSION" as const, label: "Transmission", hint: "RPC API — enabled by default on most installs.", defaultPort: "9091" },
  { value: "DELUGE" as const, label: "Deluge", hint: "WebUI password — the same one you use to log into the Deluge web interface.", defaultPort: "8112" },
  { value: "SABNZBD" as const, label: "SABnzbd", hint: "API key — find it under Config → General.", defaultPort: "8080" },
  { value: "NZBGET" as const, label: "NZBGet", hint: "Control username/password — set under Settings → Security.", defaultPort: "6789" },
];

function AddDownloadClientModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [type, setType] = useState<(typeof CLIENT_TYPES)[number]["value"]>("QBITTORRENT");
  const [name, setName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [category, setCategory] = useState("kosmos");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeType = CLIENT_TYPES.find((t) => t.value === type)!;
  const valid = name.trim().length > 0 && baseUrl.trim().length > 0;

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      await api.createDownloadClient({
        name,
        type,
        baseUrl,
        username: username || null,
        password: password || null,
        category: category || null,
      });
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
            <Download size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add download client</div>
            <div className="dialog-sub">{activeType.hint}</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Type</label>
          <div className="seg" style={{ width: "100%", height: 40 }}>
            {CLIENT_TYPES.map((t) => (
              <button
                key={t.value}
                className={type === t.value ? "active" : ""}
                style={{ flex: 1 }}
                onClick={() => {
                  setType(t.value);
                  setBaseUrl((current) => (current.trim() === "" ? "" : current.replace(/:\d+$/, `:${t.defaultPort}`)));
                }}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="field">
          <label>Name</label>
          <input
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={`Home ${activeType.label}`}
          />
        </div>

        <div className="field">
          <label>Base URL</label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder={`http://localhost:${activeType.defaultPort}`}
          />
        </div>

        <div style={{ display: "flex", gap: 14 }}>
          {type !== "DELUGE" && type !== "SABNZBD" && (
            <div className="field" style={{ flex: 1 }}>
              <label>Username</label>
              <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} />
            </div>
          )}
          <div className="field" style={{ flex: 1 }}>
            <label>{type === "SABNZBD" ? "API Key" : "Password"}</label>
            <div style={{ position: "relative" }}>
              <Key size={14} style={{ position: "absolute", left: 12, top: 13, color: "var(--text-faint)" }} />
              <input
                className="input"
                style={{ paddingLeft: 34 }}
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="field">
          <label>Category</label>
          <input className="input" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="kosmos" />
          <span className="text-faint" style={{ fontSize: 11 }}>
            Tags every grab so Kosmos can find it again — separate from anything else in the client.
          </span>
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
