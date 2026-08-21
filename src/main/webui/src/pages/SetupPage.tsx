import {
  ArrowLeftIcon as ArrowLeft,
  ArrowRightIcon as ArrowRight,
  ArrowUpRightIcon as ArrowUpRight,
  CheckIcon as Check,
  CircleNotchIcon as CircleNotch,
  CompassIcon as Compass,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  FolderPlusIcon as FolderPlus,
  KeyIcon as Key,
  LockSimpleIcon as LockSimple,
  MagicWandIcon as MagicWand,
  MagnetIcon as Magnet,
  MinusIcon as Minus,
  ArrowsLeftRightIcon as ArrowsLeftRight,
  NewspaperIcon as Newspaper,
  PlugsConnectedIcon as PlugsConnected,
  SparkleIcon as Sparkle,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../api/client";

const LABELS = ["Welcome", "Metadata source", "First indexer", "Download client", "All set"];
const CLIENTS: {
  label: "qBittorrent" | "Transmission" | "Deluge" | "SABnzbd" | "NZBGet";
  icon: typeof Magnet;
  enabled: boolean;
}[] = [
  { label: "qBittorrent", icon: Magnet, enabled: true },
  { label: "Transmission", icon: ArrowsLeftRight, enabled: true },
  { label: "Deluge", icon: Magnet, enabled: true },
  { label: "SABnzbd", icon: Newspaper, enabled: true },
  { label: "NZBGet", icon: Newspaper, enabled: true },
];

const CLIENT_TYPE_BY_LABEL: Record<(typeof CLIENTS)[number]["label"], string> = {
  qBittorrent: "QBITTORRENT",
  Transmission: "TRANSMISSION",
  Deluge: "DELUGE",
  SABnzbd: "SABNZBD",
  NZBGet: "NZBGET",
};

const DEFAULT_PORT_BY_LABEL: Record<(typeof CLIENTS)[number]["label"], string> = {
  qBittorrent: "8080",
  Transmission: "9091",
  Deluge: "8112",
  SABnzbd: "8080",
  NZBGet: "6789",
};

type TestState = "testing" | "ok" | "fail" | undefined;

interface TestMsg {
  msg: string;
  fg: string;
  icon: typeof Check;
  spin: boolean;
}

function testMsg(state: TestState, okText: string, waitText: string, failText?: string): TestMsg {
  if (state === "testing") return { msg: "Contacting…", fg: "var(--text-muted)", icon: CircleNotch, spin: true };
  if (state === "ok") return { msg: okText, fg: "#7fd6ac", icon: Check, spin: false };
  if (state === "fail") return { msg: failText ?? "No response — check the value and try again", fg: "#ee9891", icon: X, spin: false };
  return { msg: waitText, fg: "var(--text-disabled)", icon: PlugsConnected, spin: false };
}

export default function SetupPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [visited, setVisited] = useState<Record<number, boolean>>({ 0: true });
  const [showKey, setShowKey] = useState(false);
  const [tls, setTls] = useState(false);

  const [tmdbConfigured, setTmdbConfigured] = useState<boolean | null>(null);
  const [checkingTmdb, setCheckingTmdb] = useState(false);
  const [f2, setF2] = useState({ name: "", url: "", key: "" });
  const [f3, setF3] = useState({ host: "localhost", port: "8080", user: "", pass: "", client: "qBittorrent" as (typeof CLIENTS)[number]["label"] });

  const [tests, setTests] = useState<Record<number, TestState>>({});
  const [failMsg, setFailMsg] = useState<Record<number, string>>({});
  const [skipped, setSkipped] = useState<Record<number, boolean>>({});
  const [done, setDone] = useState<Record<number, boolean>>({});

  function go(next: number) {
    setStep(next);
    setVisited((v) => ({ ...v, [next]: true }));
  }

  async function checkTmdb() {
    setCheckingTmdb(true);
    try {
      const status = await api.metadataStatus();
      setTmdbConfigured(status.tmdbConfigured);
      setDone((d) => ({ ...d, 1: status.tmdbConfigured }));
    } finally {
      setCheckingTmdb(false);
    }
  }

  useEffect(() => {
    checkTmdb();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function addIndexer() {
    setTests((t) => ({ ...t, 2: "testing" }));
    try {
      await api.createIndexer({ name: f2.name.trim(), baseUrl: f2.url.trim(), apiKey: f2.key });
      setTests((t) => ({ ...t, 2: "ok" }));
      setDone((d) => ({ ...d, 2: true }));
    } catch (e) {
      setFailMsg((m) => ({ ...m, 2: e instanceof ApiError ? e.message : "Could not save this indexer" }));
      setTests((t) => ({ ...t, 2: "fail" }));
    }
  }

  async function addDownloadClient() {
    setTests((t) => ({ ...t, 3: "testing" }));
    try {
      await api.createDownloadClient({
        name: f3.client,
        type: CLIENT_TYPE_BY_LABEL[f3.client],
        baseUrl: `${tls ? "https" : "http"}://${f3.host.trim()}:${f3.port.trim()}`,
        username: f3.user.trim() || null,
        password: f3.pass || null,
        category: null,
      });
      setTests((t) => ({ ...t, 3: "ok" }));
      setDone((d) => ({ ...d, 3: true }));
    } catch (e) {
      setFailMsg((m) => ({ ...m, 3: e instanceof ApiError ? e.message : "Could not save this download client" }));
      setTests((t) => ({ ...t, 3: "fail" }));
    }
  }

  const valid = [true, true, true, true, true];

  const m1 = checkingTmdb
    ? { msg: "Checking…", fg: "var(--text-muted)", icon: CircleNotch, spin: true }
    : tmdbConfigured
      ? { msg: "Key detected in server config", fg: "#7fd6ac", icon: Check, spin: false }
      : { msg: "Not set — add KOSMOS_METADATA_TMDB_API_KEY and restart Kosmos", fg: "#ee9891", icon: X, spin: false };
  const m2 = testMsg(tests[2], "Saved — Kosmos will search this indexer", "We'll save it to Settings → Indexers", failMsg[2]);
  const m3 = testMsg(tests[3], "Saved — Kosmos will send grabs here", "We'll save it to Settings → Download Clients", failMsg[3]);

  const configured = [1, 2, 3].filter((n) => done[n]).length;
  const doneBlurb =
    configured === 3
      ? "Metadata, an indexer and a download client are all wired up. Add something and Kosmos takes it from there."
      : configured === 0
        ? "Nothing configured yet — you can browse straight away, and set sources up whenever you like."
        : "That's enough to start. The steps you skipped are waiting under Settings, unchanged.";

  function sumRow(n: number, label: string, value: string, fixStep: number) {
    const ok = !!done[n];
    return { label, value: ok ? value : "skipped", ok, fixStep };
  }

  const summary = [
    sumRow(1, "Metadata source", "TMDB · key detected", 1),
    sumRow(2, "Indexer", f2.name.trim() || "indexer", 2),
    sumRow(3, "Download client", `${f3.client} · ${f3.host}:${f3.port}`, 3),
  ];

  function next() {
    go(Math.min(4, step + 1));
  }
  function skip() {
    setSkipped((s) => ({ ...s, [step]: true }));
    go(Math.min(4, step + 1));
  }
  function back() {
    go(Math.max(0, step - 1));
  }

  const keyType = showKey ? "text" : "password";
  const KeyEye = showKey ? EyeSlash : Eye;

  return (
    <div className="setup-shell">
      <div className="setup-glow" />

      <div className="setup-topbar">
        <span className="setup-topbar-mark" />
        <span className="setup-topbar-word">Kosmos</span>
        <div style={{ flex: 1 }} />
        <span className="setup-topbar-count">{step === 4 ? "setup complete" : `step ${step + 1} of 5`}</span>
      </div>

      <div className="setup-dots">
        {LABELS.map((_, i) => (
          <div
            key={i}
            onClick={() => visited[i] && go(i)}
            className="setup-dot"
            style={{
              flex: i === step ? 2.4 : 1,
              background: i === step ? "var(--accent-gradient)" : i < step ? "rgba(145,132,217,.4)" : "var(--border)",
              cursor: visited[i] ? "pointer" : "default",
            }}
          />
        ))}
      </div>
      <div className="setup-step-label">{LABELS[step]}</div>

      <div className={`setup-content k-scroll${step === 0 ? " centered" : ""}`}>
        {step === 0 && (
          <div className="setup-step">
            <div className="setup-icon-lg" />
            <h1 style={{ fontSize: 40, letterSpacing: "-0.038em", marginBottom: 14 }}>Welcome to Kosmos.</h1>
            <p className="text-muted" style={{ maxWidth: "44ch", fontSize: 15.5, lineHeight: 1.65, marginBottom: 26 }}>
              Your movies, series and anime in one place — found, fetched and kept current, without a spreadsheet's
              worth of settings.
            </p>
            <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
              <div className="setup-promise-row">
                <span className="setup-promise-icon">
                  <MagicWand size={13} />
                </span>
                <span style={{ fontSize: 13.5, color: "var(--text-secondary)" }}>
                  Add a title once — Kosmos finds it, grabs it, and upgrades it later
                </span>
              </div>
              <div className="setup-promise-row">
                <span className="setup-promise-icon">
                  <Sparkle size={13} />
                </span>
                <span style={{ fontSize: 13.5, color: "var(--text-secondary)" }}>
                  Anime handled properly: seasons, absolute numbering, dual audio
                </span>
              </div>
              <div className="setup-promise-row">
                <span className="setup-promise-icon">
                  <LockSimple size={13} />
                </span>
                <span style={{ fontSize: 13.5, color: "var(--text-secondary)" }}>
                  Self-hosted. Nothing leaves your server but metadata lookups
                </span>
              </div>
            </div>
            <div className="setup-footnote">Four short steps, about two minutes. Everything is changeable later.</div>
          </div>
        )}

        {step === 1 && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Where should titles come from?</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 26 }}>
              TMDB provides posters, backdrops and cast for movies and series. A free key takes a minute to create.
            </p>

            <div className="setup-provider-card">
              <div className="setup-provider-header">
                <span className="setup-provider-badge">TM</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, fontSize: 14 }}>TMDB</div>
                  <div className="text-faint" style={{ fontSize: 11.5, marginTop: 3 }}>
                    Movies, series, artwork and cast
                  </div>
                </div>
                <a href="https://www.themoviedb.org/settings/api" target="_blank" rel="noreferrer" style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 11.5, fontWeight: 500, whiteSpace: "nowrap" }}>
                  Get a key
                  <ArrowUpRight size={12} />
                </a>
              </div>
              <p className="text-faint" style={{ fontSize: 12, lineHeight: 1.6, marginBottom: 12 }}>
                The key is a server-side setting, not something typed into the browser: set{" "}
                <code style={{ fontFamily: "var(--font-mono)" }}>KOSMOS_METADATA_TMDB_API_KEY</code> in Kosmos's
                environment and restart it. This step just confirms whether that's already done.
              </p>
              <div className="setup-test-row">
                <span className="setup-test-btn" onClick={checkTmdb}>
                  <m1.icon size={13} className={m1.spin ? "spin" : ""} />
                  Re-check
                </span>
                <span style={{ fontSize: 11.5, color: m1.fg }}>{m1.msg}</span>
              </div>
            </div>

          </div>
        )}

        {step === 2 && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Add your first indexer.</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 26 }}>
              This is where Kosmos looks for releases. One is enough to start — add the rest later, or point it at
              Prowlarr and inherit them all.
            </p>

            <div style={{ display: "flex", flexDirection: "column", gap: 14, opacity: done[2] ? 0.6 : 1 }}>
              <div>
                <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Name</label>
                <input
                  className="input"
                  disabled={done[2]}
                  value={f2.name}
                  onChange={(e) => {
                    setF2((s) => ({ ...s, name: e.target.value }));
                    setTests((t) => ({ ...t, 2: undefined }));
                  }}
                  placeholder="NZBGeek"
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>
                  Base URL (Torznab)
                </label>
                <input
                  className="input"
                  disabled={done[2]}
                  style={{ fontFamily: "var(--font-mono)" }}
                  value={f2.url}
                  onChange={(e) => {
                    setF2((s) => ({ ...s, url: e.target.value }));
                    setTests((t) => ({ ...t, 2: undefined }));
                  }}
                  placeholder="https://api.nzbgeek.info"
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>API key</label>
                <div className="setup-key-field">
                  <Key size={15} color="var(--text-faint)" />
                  <input
                    type={keyType}
                    disabled={done[2]}
                    value={f2.key}
                    onChange={(e) => setF2((s) => ({ ...s, key: e.target.value }))}
                    placeholder="paste key"
                  />
                  <span className="setup-key-toggle" onClick={() => setShowKey((v) => !v)}>
                    <KeyEye size={15} />
                  </span>
                </div>
              </div>
              <div className="setup-test-row">
                <span
                  className="setup-test-btn"
                  onClick={done[2] ? undefined : addIndexer}
                  style={done[2] ? { cursor: "default", opacity: 0.7 } : undefined}
                >
                  <m2.icon size={13} className={m2.spin ? "spin" : ""} />
                  {done[2] ? "Added" : "Add indexer"}
                </span>
                <span style={{ fontSize: 11.5, color: m2.fg }}>{m2.msg}</span>
              </div>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Who does the downloading?</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 22 }}>
              Kosmos hands releases to your existing client and watches the transfer. It never seeds or stores
              anything itself.
            </p>

            <div className="setup-chip-row">
              {CLIENTS.map(({ label, icon: Icon, enabled }) => (
                <span
                  key={label}
                  className={`setup-chip${f3.client === label ? " active" : ""}`}
                  style={enabled ? undefined : { opacity: 0.45, cursor: "default" }}
                  title={enabled ? undefined : "Not wired up yet — qBittorrent only for now"}
                  onClick={() =>
                    enabled &&
                    setF3((s) => ({
                      ...s,
                      client: label,
                      port: DEFAULT_PORT_BY_LABEL[label],
                    }))
                  }
                >
                  <Icon size={14} />
                  {label}
                  {!enabled && <span style={{ fontSize: 9.5 }}>soon</span>}
                </span>
              ))}
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 14, opacity: done[3] ? 0.6 : 1 }}>
              <div className="setup-grid-2" style={{ gridTemplateColumns: "1fr 106px" }}>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Host</label>
                  <input
                    className="input"
                    disabled={done[3]}
                    style={{ fontFamily: "var(--font-mono)" }}
                    value={f3.host}
                    onChange={(e) => {
                      setF3((s) => ({ ...s, host: e.target.value }));
                      setTests((t) => ({ ...t, 3: undefined }));
                    }}
                    placeholder="localhost"
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Port</label>
                  <input
                    className="input"
                    disabled={done[3]}
                    style={{ fontFamily: "var(--font-mono)" }}
                    value={f3.port}
                    onChange={(e) => {
                      setF3((s) => ({ ...s, port: e.target.value.replace(/[^0-9]/g, "") }));
                      setTests((t) => ({ ...t, 3: undefined }));
                    }}
                  />
                </div>
              </div>
              <div className="setup-grid-2" style={{ gridTemplateColumns: "1fr 1fr" }}>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Username</label>
                  <input
                    className="input"
                    disabled={done[3]}
                    value={f3.user}
                    onChange={(e) => setF3((s) => ({ ...s, user: e.target.value }))}
                    placeholder="admin"
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Password</label>
                  <div className="setup-key-field">
                    <input
                      type={keyType}
                      disabled={done[3]}
                      value={f3.pass}
                      onChange={(e) => setF3((s) => ({ ...s, pass: e.target.value }))}
                      placeholder="••••••••"
                    />
                    <span className="setup-key-toggle" onClick={() => setShowKey((v) => !v)}>
                      <KeyEye size={15} />
                    </span>
                  </div>
                </div>
              </div>
              <div className="setup-toggle-row">
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, fontSize: 12.5 }}>Connect over HTTPS</div>
                  <div className="text-faint" style={{ fontSize: 11, marginTop: 2 }}>
                    Only if your client has a certificate configured.
                  </div>
                </div>
                <button type="button" className={`toggle${tls ? " on" : ""}`} disabled={done[3]} onClick={() => setTls((v) => !v)} />
              </div>
              <div className="setup-test-row">
                <span
                  className="setup-test-btn"
                  onClick={done[3] ? undefined : addDownloadClient}
                  style={done[3] ? { cursor: "default", opacity: 0.7 } : undefined}
                >
                  <m3.icon size={13} className={m3.spin ? "spin" : ""} />
                  {done[3] ? "Added" : "Add download client"}
                </span>
                <span style={{ fontSize: 11.5, color: m3.fg }}>{m3.msg}</span>
              </div>
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="setup-step">
            <span style={{ display: "grid", placeItems: "center", width: 52, height: 52, borderRadius: 16, background: "rgba(79,191,139,.13)", border: "1px solid rgba(79,191,139,.3)", color: "#7fd6ac", marginBottom: 26 }}>
              <Check size={25} />
            </span>
            <h1 style={{ fontSize: 32, letterSpacing: "-0.03em", marginBottom: 13 }}>Kosmos is ready.</h1>
            <p className="text-muted" style={{ maxWidth: "46ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 26 }}>
              {doneBlurb}
            </p>

            <div className="setup-summary">
              {summary.map((row) => (
                <div key={row.label} className="setup-summary-row">
                  <span
                    className="setup-summary-icon"
                    style={{
                      background: row.ok ? "rgba(79,191,139,.16)" : "rgba(233,233,237,.07)",
                      color: row.ok ? "#7fd6ac" : "var(--text-faint)",
                    }}
                  >
                    {row.ok ? <Check size={12} /> : <Minus size={12} />}
                  </span>
                  <span style={{ flex: 1, minWidth: 0, fontWeight: 500, fontSize: 12.5, color: row.ok ? "var(--text)" : "var(--text-muted)" }}>
                    {row.label}
                  </span>
                  <span className="setup-summary-value">{row.value}</span>
                  {!row.ok && (
                    <span style={{ fontSize: 11.5, fontWeight: 500, color: "var(--accent-2)", cursor: "pointer", flex: "none" }} onClick={() => go(row.fixStep)}>
                      Set up
                    </span>
                  )}
                </div>
              ))}
            </div>

            <div style={{ display: "flex", gap: 11, flexWrap: "wrap" }}>
              <button type="button" className="btn btn-hero" onClick={() => navigate("/")}>
                <Compass size={16} />
                Take me to Discover
              </button>
              <button type="button" className="btn btn-secondary" onClick={() => navigate("/")}>
                <FolderPlus size={16} />
                Scan a folder first
              </button>
            </div>
          </div>
        )}
      </div>

      {step !== 4 && (
        <div className="setup-footer">
          {step > 0 ? (
            <button type="button" className="setup-back" onClick={back}>
              <ArrowLeft size={14} />
              Back
            </button>
          ) : (
            <span />
          )}
          <div style={{ flex: 1 }} />
          {step > 0 && step < 4 && (
            <button type="button" className="setup-skip" onClick={skip}>
              Skip for now
            </button>
          )}
          <button
            type="button"
            className="setup-next"
            onClick={next}
            style={{
              background: valid[step] ? "var(--accent-gradient)" : "rgba(233,233,237,.09)",
              color: valid[step] ? "#0b0c12" : "var(--text-faint)",
            }}
          >
            {step === 0 ? "Get started" : step === 3 ? "Finish" : "Continue"}
            <ArrowRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}
