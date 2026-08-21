import {
  BellIcon as Bell,
  CheckCircleIcon as CheckCircle,
  DiscordLogoIcon as DiscordLogo,
  PlusIcon as Plus,
  TelegramLogoIcon as TelegramLogo,
  WebhooksLogoIcon as WebhooksLogo,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";

const TYPE_ICON: Record<string, JSX.Element> = {
  DISCORD: <DiscordLogo size={16} />,
  TELEGRAM: <TelegramLogo size={16} />,
  WEBHOOK: <WebhooksLogo size={16} />,
};

export default function NotificationsPage() {
  const { data: notifiers, error: loadError, reload } = useApi(() => api.listNotifiers(), []);
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
          <h2 style={{ marginBottom: 6 }}>Notifications</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Get pinged when a movie finishes importing. Discord, Telegram, and generic webhooks.
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setModalOpen(true)}>
          <Plus size={15} weight="bold" />
          Add Notifier
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load notifiers: {loadError}</p>}
      {notifiers?.length === 0 && <p className="text-muted">No notifiers configured yet.</p>}

      {notifiers?.map((notifier) => (
        <div key={notifier.id} className={`indexer-row${notifier.enabled ? "" : " disabled"}`}>
          <span className="icon-tile">{TYPE_ICON[notifier.type] ?? <Bell size={16} />}</span>
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name">{notifier.name}</span>
              <span className="protocol-pill">{notifier.type}</span>
            </div>
            <div className="indexer-row-sub">
              <span>{notifier.urlSet ? "URL set" : notifier.tokenSet ? "token set" : "not configured"}</span>
              {notifier.target && (
                <>
                  <span className="text-faint">·</span>
                  <span>target: {notifier.target}</span>
                </>
              )}
            </div>
          </div>
        </div>
      ))}

      {modalOpen && (
        <AddNotifierModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
            showToast("Notifier added");
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

function AddNotifierModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [type, setType] = useState<"DISCORD" | "TELEGRAM" | "WEBHOOK">("WEBHOOK");
  const [url, setUrl] = useState("");
  const [token, setToken] = useState("");
  const [target, setTarget] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const needsUrl = type === "DISCORD" || type === "WEBHOOK";
  const valid = name.trim().length > 0 && (needsUrl ? url.trim().length > 0 : token.trim().length > 0);

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      await api.createNotifier({
        name,
        type,
        url: needsUrl ? url : null,
        token: type === "TELEGRAM" ? token : null,
        target: type === "TELEGRAM" ? target : null,
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
            <Bell size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add notifier</div>
            <div className="dialog-sub">A plain webhook POST — no Python, no extra service.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="My Discord" />
        </div>

        <div className="field">
          <label>Type</label>
          <div className="seg">
            <button type="button" className={type === "DISCORD" ? "active" : ""} onClick={() => setType("DISCORD")}>
              Discord
            </button>
            <button type="button" className={type === "TELEGRAM" ? "active" : ""} onClick={() => setType("TELEGRAM")}>
              Telegram
            </button>
            <button type="button" className={type === "WEBHOOK" ? "active" : ""} onClick={() => setType("WEBHOOK")}>
              Webhook
            </button>
          </div>
        </div>

        {needsUrl ? (
          <div className="field">
            <label>{type === "DISCORD" ? "Webhook URL" : "URL"}</label>
            <input
              className="input"
              style={{ fontFamily: "var(--font-mono)" }}
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder={type === "DISCORD" ? "https://discord.com/api/webhooks/…" : "https://example.com/hook"}
            />
          </div>
        ) : (
          <>
            <div className="field">
              <label>Bot token</label>
              <input className="input" style={{ fontFamily: "var(--font-mono)" }} value={token} onChange={(e) => setToken(e.target.value)} />
            </div>
            <div className="field">
              <label>Chat ID</label>
              <input className="input" value={target} onChange={(e) => setTarget(e.target.value)} placeholder="123456789" />
            </div>
          </>
        )}

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
