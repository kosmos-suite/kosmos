import {
  ArrowsClockwiseIcon as ArrowsClockwise,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  CheckCircleIcon as CheckCircle,
  FilmSlateIcon as FilmSlate,
  KeyIcon as Key,
  PlugsIcon as Plugs,
  PlusIcon as Plus,
  UsersIcon as Users,
  WarningCircleIcon as WarningCircle,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { api, ApiError } from "../../api/client";
import type { JellyfinServer, ScheduledJob } from "../../api/types";
import { JobProgressBar } from "../../components/JobProgressBar";
import { useApi } from "../../hooks/useApi";
import { useJobPoll } from "../../hooks/useJobPoll";

export default function JellyfinPage() {
  const { data: servers, error: loadError, setData: setServers, reload } = useApi(
    () => api.listJellyfinServers(),
    [],
  );
  const [modalOpen, setModalOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((current) => (current === message ? null : current)), 6000);
  }

  function patchServer(id: string, patch: Partial<JellyfinServer>) {
    setServers((current) => current?.map((s) => (s.id === id ? { ...s, ...patch } : s)) ?? current);
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Jellyfin</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Connect an already-scanned Jellyfin library. Library sync marks what you already have
            as available; user import brings in accounts to sign into Kosmos with — each runs (and
            can be scheduled) on its own, over just the libraries or users you pick.
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
        <JellyfinServerRow
          key={server.id}
          server={server}
          onServerUpdate={(patch) => patchServer(server.id, patch)}
          showToast={showToast}
        />
      ))}

      {modalOpen && (
        <AddJellyfinServerModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
            showToast("Jellyfin server added — pick its libraries and users below to sync.");
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

function JellyfinServerRow({
  server,
  onServerUpdate,
  showToast,
}: {
  server: JellyfinServer;
  onServerUpdate: (patch: Partial<JellyfinServer>) => void;
  showToast: (message: string) => void;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`indexer-row${server.enabled ? "" : " disabled"}`} style={{ flexDirection: "column", alignItems: "stretch" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, cursor: "pointer" }} onClick={() => setExpanded((v) => !v)}>
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
        <CaretDown size={14} className={`job-row-chevron${expanded ? " open" : ""}`} />
      </div>

      {expanded && (
        <div style={{ display: "flex", flexDirection: "column", gap: 18, marginTop: 16, paddingTop: 16, borderTop: "1px solid var(--border-subtle)" }}>
          <LibrarySelectionPanel server={server} onServerUpdate={onServerUpdate} showToast={showToast} />
          <UserSelectionPanel server={server} onServerUpdate={onServerUpdate} showToast={showToast} />
        </div>
      )}
    </div>
  );
}

function LibrarySelectionPanel({
  server,
  onServerUpdate,
  showToast,
}: {
  server: JellyfinServer;
  onServerUpdate: (patch: Partial<JellyfinServer>) => void;
  showToast: (message: string) => void;
}) {
  const { data: libraries, loading, error } = useApi(() => api.listJellyfinLibraries(server.id), [server.id]);
  const [selected, setSelected] = useState<Set<string>>(new Set(server.selectedLibraryIds));
  const [clicking, setClicking] = useState(false);
  const [liveJob, setLiveJob] = useState<ScheduledJob | null>(null);

  const jobName = `jellyfin-library-sync-${server.id}`;
  // Ambient truth (another tab, another user, or the schedule could have started this), not just
  // whether this component's own click is still in flight.
  const running = clicking || liveJob?.running === true;
  useJobPoll(jobName, true, setLiveJob);

  // Empty selection means "every library" — same convention the backend uses.
  const allSelected = selected.size === 0;

  async function toggle(id: string) {
    const next = new Set(selected);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelected(next);
    const ids = Array.from(next);
    await api.updateJellyfinLibrarySelection(server.id, ids);
    onServerUpdate({ selectedLibraryIds: ids });
  }

  async function selectAll() {
    setSelected(new Set());
    await api.updateJellyfinLibrarySelection(server.id, []);
    onServerUpdate({ selectedLibraryIds: [] });
  }

  async function runSync() {
    if (running) return; // already running — from this tab, another tab, or the schedule
    setClicking(true);
    try {
      const run = await api.syncJellyfinLibraries(server.id);
      showToast(
        run.status === "FAILED"
          ? `Library sync failed${run.message ? `: ${run.message}` : ""}`
          : (run.message ?? "Library sync complete."),
      );
    } catch (e) {
      // 409 means it was already running by the time the request landed — not a real failure,
      // and the progress bar (driven by useJobPoll) already reflects it.
      if (!(e instanceof ApiError && e.status === 409)) {
        showToast(e instanceof ApiError ? `Library sync failed: ${e.message}` : "Library sync failed");
      }
    } finally {
      setClicking(false);
    }
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 10 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <FilmSlate size={15} className="text-faint" />
          <span className="section-label" style={{ margin: 0 }}>
            Libraries
          </span>
        </div>
        <button type="button" className="btn btn-secondary" onClick={runSync} disabled={running}>
          <ArrowsClockwise size={14} className={running ? "spin" : ""} />
          {running ? "Syncing…" : "Sync Libraries"}
        </button>
      </div>

      {loading && <p className="text-faint" style={{ fontSize: 12 }}>Loading libraries…</p>}
      {error && <p className="text-muted" style={{ fontSize: 12 }}>Could not load libraries: {error}</p>}
      {libraries?.length === 0 && <p className="text-faint" style={{ fontSize: 12 }}>No libraries found on this server.</p>}

      {libraries && libraries.length > 0 && (
        <>
          <div className="setup-test-row" style={{ marginTop: 0, marginBottom: 8 }}>
            <span className="setup-test-btn" onClick={selectAll}>
              {allSelected ? "All libraries selected" : "Select all"}
            </span>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            {libraries.map((library) => {
              const checked = allSelected || selected.has(library.id);
              return (
                <div key={library.id} onClick={() => toggle(library.id)} className="setup-toggle-row" style={{ cursor: "pointer" }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500, fontSize: 13 }}>{library.name}</div>
                    {library.collectionType && (
                      <div className="text-faint" style={{ fontSize: 11, marginTop: 2 }}>
                        {library.collectionType}
                      </div>
                    )}
                  </div>
                  <span
                    style={{
                      display: "grid",
                      placeItems: "center",
                      width: 20,
                      height: 20,
                      borderRadius: 6,
                      border: checked ? "none" : "1px solid var(--border)",
                      background: checked ? "var(--accent-gradient)" : "transparent",
                      color: "#0b0c12",
                      flex: "none",
                    }}
                  >
                    {checked && <Check size={13} weight="bold" />}
                  </span>
                </div>
              );
            })}
          </div>
        </>
      )}

      {running && <JobProgressBar job={liveJob} />}
    </div>
  );
}

function UserSelectionPanel({
  server,
  onServerUpdate,
  showToast,
}: {
  server: JellyfinServer;
  onServerUpdate: (patch: Partial<JellyfinServer>) => void;
  showToast: (message: string) => void;
}) {
  const { data: users, loading, error } = useApi(() => api.listJellyfinUsers(server.id), [server.id]);
  const [selected, setSelected] = useState<Set<string>>(new Set(server.selectedUserIds));
  const [clicking, setClicking] = useState(false);
  const [liveJob, setLiveJob] = useState<ScheduledJob | null>(null);

  const jobName = `jellyfin-user-import-${server.id}`;
  const running = clicking || liveJob?.running === true;
  useJobPoll(jobName, true, setLiveJob);

  // Empty selection means "every account" — same convention the backend uses.
  const allSelected = selected.size === 0;

  async function toggle(id: string) {
    const next = new Set(selected);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelected(next);
    const ids = Array.from(next);
    await api.updateJellyfinUserSelection(server.id, ids);
    onServerUpdate({ selectedUserIds: ids });
  }

  async function selectAll() {
    setSelected(new Set());
    await api.updateJellyfinUserSelection(server.id, []);
    onServerUpdate({ selectedUserIds: [] });
  }

  async function runImport() {
    if (running) return; // already running — from this tab, another tab, or the schedule
    setClicking(true);
    try {
      const run = await api.syncJellyfinUsers(server.id);
      showToast(
        run.status === "FAILED"
          ? `User import failed${run.message ? `: ${run.message}` : ""}`
          : (run.message ?? "User import complete."),
      );
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 409)) {
        showToast(e instanceof ApiError ? `User import failed: ${e.message}` : "User import failed");
      }
    } finally {
      setClicking(false);
    }
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 10 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <Users size={15} className="text-faint" />
          <span className="section-label" style={{ margin: 0 }}>
            Users
          </span>
        </div>
        <button type="button" className="btn btn-secondary" onClick={runImport} disabled={running}>
          <ArrowsClockwise size={14} className={running ? "spin" : ""} />
          {running ? "Importing…" : "Import Users"}
        </button>
      </div>

      {loading && <p className="text-faint" style={{ fontSize: 12 }}>Loading users…</p>}
      {error && <p className="text-muted" style={{ fontSize: 12 }}>Could not load users: {error}</p>}
      {users?.length === 0 && <p className="text-faint" style={{ fontSize: 12 }}>No user accounts found on this server.</p>}

      {users && users.length > 0 && (
        <>
          <div className="setup-test-row" style={{ marginTop: 0, marginBottom: 8 }}>
            <span className="setup-test-btn" onClick={selectAll}>
              {allSelected ? "All users selected" : "Select all"}
            </span>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            {users.map((user) => {
              const checked = allSelected || selected.has(user.id);
              return (
                <div key={user.id} onClick={() => toggle(user.id)} className="setup-toggle-row" style={{ cursor: "pointer" }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500, fontSize: 13 }}>{user.name}</div>
                    {user.isAdmin && (
                      <div className="text-faint" style={{ fontSize: 11, marginTop: 2 }}>
                        Administrator
                      </div>
                    )}
                  </div>
                  <span
                    style={{
                      display: "grid",
                      placeItems: "center",
                      width: 20,
                      height: 20,
                      borderRadius: 6,
                      border: checked ? "none" : "1px solid var(--border)",
                      background: checked ? "var(--accent-gradient)" : "transparent",
                      color: "#0b0c12",
                      flex: "none",
                    }}
                  >
                    {checked && <Check size={13} weight="bold" />}
                  </span>
                </div>
              );
            })}
          </div>
        </>
      )}

      {running && <JobProgressBar job={liveJob} />}
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

  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null);
  const [testedKey, setTestedKey] = useState<string | null>(null);

  // Re-keyed on every baseUrl/apiKey edit — a passing test only counts for the values it actually
  // checked, so editing either field after a successful test silently un-verifies the form again.
  const connectionKey = `${baseUrl.trim()}|${apiKey.trim()}`;
  const verified = testResult?.ok === true && testedKey === connectionKey;
  const showTestResult = testResult !== null && testedKey === connectionKey;

  const canTest = baseUrl.trim().length > 0 && apiKey.trim().length > 0 && !testing;
  const valid = name.trim().length > 0 && verified;

  async function testConnection() {
    if (!canTest) return;
    setTesting(true);
    try {
      const result = await api.testJellyfinConnection({ baseUrl: baseUrl.trim(), apiKey: apiKey.trim() });
      setTestedKey(connectionKey);
      setTestResult({ ok: result.ok, message: result.message });
    } catch (e) {
      setTestedKey(connectionKey);
      setTestResult({ ok: false, message: e instanceof Error ? e.message : "Connection failed" });
    } finally {
      setTesting(false);
    }
  }

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

        <div className="field">
          <button type="button" className="btn btn-secondary" onClick={testConnection} disabled={!canTest}>
            <Plugs size={14} />
            {testing ? "Testing…" : "Test Connection"}
          </button>
          {showTestResult && (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 7,
                marginTop: 8,
                fontSize: 12.5,
                color: testResult?.ok ? "var(--status-good-text)" : "var(--status-bad-text)",
              }}
            >
              {testResult?.ok ? <CheckCircle size={14} weight="fill" /> : <WarningCircle size={14} weight="fill" />}
              {testResult?.message}
            </div>
          )}
          {!showTestResult && (
            <p className="text-faint" style={{ fontSize: 11.5, marginTop: 8 }}>
              A successful test is required before this server can be saved.
            </p>
          )}
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
