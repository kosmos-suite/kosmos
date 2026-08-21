import {
  ArrowLeftIcon as ArrowLeft,
  ArrowRightIcon as ArrowRight,
  ArrowUpRightIcon as ArrowUpRight,
  CheckIcon as Check,
  CircleNotchIcon as CircleNotch,
  CompassIcon as Compass,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  FolderOpenIcon as FolderOpen,
  FolderPlusIcon as FolderPlus,
  KeyIcon as Key,
  LockSimpleIcon as LockSimple,
  MagicWandIcon as MagicWand,
  MagnetIcon as Magnet,
  MinusIcon as Minus,
  ArrowsClockwiseIcon as ArrowsClockwise,
  NewspaperIcon as Newspaper,
  PlugsConnectedIcon as PlugsConnected,
  SparkleIcon as Sparkle,
  WarningIcon as Warning,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { FolderBrowserModal } from "../components/FolderBrowserModal";
import type { LibraryContentType, LibraryRootFolder } from "../api/types";

const CONTENT_TYPES: { value: LibraryContentType; label: string }[] = [
  { value: "movie", label: "Movies" },
  { value: "show", label: "Shows" },
  { value: "anime", label: "Anime" },
];

type StepKey =
  | "welcome"
  | "account"
  | "jellyfin-libraries"
  | "metadata"
  | "indexer"
  | "download-client"
  | "media-folder"
  | "done";

const LABELS: Record<StepKey, string> = {
  welcome: "Welcome",
  account: "Account",
  "jellyfin-libraries": "Libraries",
  metadata: "Metadata source",
  indexer: "First indexer",
  "download-client": "Download client",
  "media-folder": "Media folder",
  done: "All set",
};

const CLIENTS: {
  label: "qBittorrent" | "Transmission" | "Deluge" | "SABnzbd" | "NZBGet";
  icon: typeof Magnet;
  isTorrent: boolean;
}[] = [
  { label: "qBittorrent", icon: Magnet, isTorrent: true },
  { label: "Transmission", icon: ArrowsClockwise, isTorrent: true },
  { label: "Deluge", icon: Magnet, isTorrent: true },
  { label: "SABnzbd", icon: Newspaper, isTorrent: false },
  { label: "NZBGet", icon: Newspaper, isTorrent: false },
];

const CLIENT_TYPE_BY_LABEL: Record<(typeof CLIENTS)[number]["label"], string> = {
  qBittorrent: "QBITTORRENT",
  Transmission: "TRANSMISSION",
  Deluge: "DELUGE",
  SABnzbd: "SABNZBD",
  NZBGet: "NZBGET",
};

const DEFAULT_BASE_URL_BY_LABEL: Record<(typeof CLIENTS)[number]["label"], string> = {
  qBittorrent: "http://localhost:8080",
  Transmission: "http://localhost:9091",
  Deluge: "http://localhost:8112",
  SABnzbd: "http://localhost:8080",
  NZBGet: "http://localhost:6789",
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
  if (state === "ok") return { msg: okText, fg: "var(--status-good-text)", icon: Check, spin: false };
  if (state === "fail") return { msg: failText ?? "No response — check the value and try again", fg: "var(--status-bad-text)", icon: X, spin: false };
  return { msg: waitText, fg: "var(--text-disabled)", icon: PlugsConnected, spin: false };
}

export default function SetupPage() {
  const navigate = useNavigate();
  const { user, loading: authLoading, login } = useAuth();

  const [needsSetup, setNeedsSetup] = useState<boolean | null>(null);
  const [jellyfinServerId, setJellyfinServerId] = useState<string | null>(null);

  useEffect(() => {
    api
      .setupStatus()
      .then((status) => setNeedsSetup(status.needsSetup))
      .catch(() => setNeedsSetup(false));
  }, []);

  const steps: StepKey[] = [
    "welcome",
    ...(needsSetup ? (["account"] as StepKey[]) : []),
    ...(needsSetup && jellyfinServerId ? (["jellyfin-libraries"] as StepKey[]) : []),
    "metadata",
    "indexer",
    "download-client",
    "media-folder",
    "done",
  ];

  const [stepIndex, setStepIndex] = useState(0);
  const [visited, setVisited] = useState<Record<number, boolean>>({ 0: true });
  const [showKey, setShowKey] = useState(false);

  const [tmdbConfigured, setTmdbConfigured] = useState<boolean | null>(null);
  const [checkingTmdb, setCheckingTmdb] = useState(false);
  const [tmdbTestState, setTmdbTestState] = useState<"untested" | "testing" | "ok" | "fail">("untested");
  const [f2, setF2] = useState({ name: "", url: "", key: "" });
  const [prowlarr, setProwlarr] = useState({ url: "", key: "" });
  const [indexerMode, setIndexerMode] = useState<"manual" | "prowlarr">("manual");
  const [f3, setF3] = useState({ baseUrl: "", user: "", pass: "", client: "qBittorrent" as (typeof CLIENTS)[number]["label"] });
  const [rootFolders, setRootFolders] = useState<LibraryRootFolder[] | null>(null);
  const [browsingFolder, setBrowsingFolder] = useState(false);
  const [savingFolder, setSavingFolder] = useState(false);
  const [folderError, setFolderError] = useState<string | null>(null);
  const [removingFolderId, setRemovingFolderId] = useState<string | null>(null);
  const [jellyfinFolderNote, setJellyfinFolderNote] = useState<string | null>(null);

  const [tests, setTests] = useState<Partial<Record<StepKey, TestState>>>({});
  const [failMsg, setFailMsg] = useState<Partial<Record<StepKey, string>>>({});
  const [skipped, setSkipped] = useState<Partial<Record<StepKey, boolean>>>({});
  const [done, setDone] = useState<Partial<Record<StepKey, boolean>>>({});
  const [prowlarrResultMsg, setProwlarrResultMsg] = useState<string | null>(null);
  const [indexerTestResult, setIndexerTestResult] = useState<{ ok: boolean; msg: string } | null>(null);
  const [testingIndexer, setTestingIndexer] = useState(false);
  const [prowlarrTestResult, setProwlarrTestResult] = useState<{ ok: boolean; msg: string } | null>(null);
  const [testingProwlarr, setTestingProwlarr] = useState(false);
  const [downloadTestResult, setDownloadTestResult] = useState<{ ok: boolean; msg: string } | null>(null);
  const [testingConnection, setTestingConnection] = useState(false);

  function go(next: number) {
    setStepIndex(next);
    setVisited((v) => ({ ...v, [next]: true }));
  }

  const step = steps[stepIndex] ?? "welcome";

  async function checkTmdb() {
    setCheckingTmdb(true);
    setTmdbTestState("untested");
    try {
      const status = await api.metadataStatus();
      setTmdbConfigured(status.tmdbConfigured);
      setDone((d) => ({ ...d, metadata: status.tmdbConfigured }));
    } finally {
      setCheckingTmdb(false);
    }
  }

  async function testTmdbConnection() {
    setTmdbTestState("testing");
    try {
      const result = await api.testTmdb();
      setTmdbTestState(result.ok ? "ok" : "fail");
    } catch {
      setTmdbTestState("fail");
    }
  }

  function reloadRootFolders() {
    api
      .listRootFolders()
      .then((folders) => {
        setRootFolders(folders);
        setDone((d) => ({ ...d, "media-folder": folders.length > 0 }));
      })
      .catch(() => undefined);
  }

  useEffect(() => {
    checkTmdb();
    reloadRootFolders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function addRootFolder(path: string, contentTypes: LibraryContentType[]) {
    setBrowsingFolder(false);
    setSavingFolder(true);
    setFolderError(null);
    try {
      await api.createRootFolder(path, contentTypes);
      reloadRootFolders();
    } catch (e) {
      setFolderError(e instanceof ApiError ? e.message : "Could not add this root folder");
    } finally {
      setSavingFolder(false);
    }
  }

  async function removeRootFolder(id: string) {
    setRemovingFolderId(id);
    try {
      await api.deleteRootFolder(id);
      reloadRootFolders();
    } catch (e) {
      setFolderError(e instanceof ApiError ? e.message : "Could not remove this root folder");
    } finally {
      setRemovingFolderId(null);
    }
  }

  if (needsSetup === false && !authLoading && !user) {
    navigate("/login", { replace: true });
    return null;
  }

  async function testIndexerConnection() {
    setTestingIndexer(true);
    setIndexerTestResult(null);
    try {
      const result = await api.testIndexer({ baseUrl: f2.url.trim(), apiKey: f2.key });
      setIndexerTestResult({ ok: result.ok, msg: result.message });
    } catch (e) {
      setIndexerTestResult({ ok: false, msg: e instanceof ApiError ? e.message : "Could not reach this indexer" });
    } finally {
      setTestingIndexer(false);
    }
  }

  async function addIndexer() {
    setTests((t) => ({ ...t, indexer: "testing" }));
    try {
      await api.createIndexer({ name: f2.name.trim(), baseUrl: f2.url.trim(), apiKey: f2.key });
      setTests((t) => ({ ...t, indexer: "ok" }));
      setDone((d) => ({ ...d, indexer: true }));
    } catch (e) {
      setFailMsg((m) => ({ ...m, indexer: e instanceof ApiError ? e.message : "Could not save this indexer" }));
      setTests((t) => ({ ...t, indexer: "fail" }));
    }
  }

  async function testProwlarrConn() {
    setTestingProwlarr(true);
    setProwlarrTestResult(null);
    try {
      const result = await api.testProwlarrConnection({ baseUrl: prowlarr.url.trim(), apiKey: prowlarr.key });
      setProwlarrTestResult({ ok: result.ok, msg: result.message });
    } catch (e) {
      setProwlarrTestResult({ ok: false, msg: e instanceof ApiError ? e.message : "Could not reach Prowlarr" });
    } finally {
      setTestingProwlarr(false);
    }
  }

  async function importProwlarr() {
    setTests((t) => ({ ...t, indexer: "testing" }));
    setProwlarrResultMsg(null);
    try {
      const result = await api.importIndexersFromProwlarr({ baseUrl: prowlarr.url.trim(), apiKey: prowlarr.key });
      setProwlarrResultMsg(
        `Imported ${result.imported} indexer${result.imported === 1 ? "" : "s"}${result.skippedDisabled ? ` (${result.skippedDisabled} disabled in Prowlarr, skipped)` : ""}.`,
      );
      setTests((t) => ({ ...t, indexer: "ok" }));
      setDone((d) => ({ ...d, indexer: true }));
    } catch (e) {
      setFailMsg((m) => ({ ...m, indexer: e instanceof ApiError ? e.message : "Could not reach Prowlarr" }));
      setTests((t) => ({ ...t, indexer: "fail" }));
    }
  }

  async function testDownloadClient() {
    setTestingConnection(true);
    setDownloadTestResult(null);
    try {
      const result = await api.testDownloadClient({
        type: CLIENT_TYPE_BY_LABEL[f3.client],
        baseUrl: f3.baseUrl.trim(),
        username: f3.user.trim() || null,
        password: f3.pass || null,
      });
      setDownloadTestResult({ ok: result.ok, msg: result.message });
    } catch (e) {
      setDownloadTestResult({ ok: false, msg: e instanceof ApiError ? e.message : "Could not reach this client" });
    } finally {
      setTestingConnection(false);
    }
  }

  async function addDownloadClient() {
    setTests((t) => ({ ...t, "download-client": "testing" }));
    try {
      await api.createDownloadClient({
        name: f3.client,
        type: CLIENT_TYPE_BY_LABEL[f3.client],
        baseUrl: f3.baseUrl.trim(),
        username: f3.user.trim() || null,
        password: f3.pass || null,
        category: null,
      });
      setTests((t) => ({ ...t, "download-client": "ok" }));
      setDone((d) => ({ ...d, "download-client": true }));
    } catch (e) {
      setFailMsg((m) => ({ ...m, "download-client": e instanceof ApiError ? e.message : "Could not save this download client" }));
      setTests((t) => ({ ...t, "download-client": "fail" }));
    }
  }

  const m1 = checkingTmdb
    ? { msg: "Checking…", fg: "var(--text-muted)", icon: CircleNotch, spin: true }
    : !tmdbConfigured
      ? { msg: "Not set — add KOSMOS_METADATA_TMDB_API_KEY and restart Kosmos", fg: "var(--status-bad-text)", icon: X, spin: false }
      : tmdbTestState === "testing"
        ? { msg: "Contacting TMDB…", fg: "var(--text-muted)", icon: CircleNotch, spin: true }
        : tmdbTestState === "ok"
          ? { msg: "Key verified working", fg: "var(--status-good-text)", icon: Check, spin: false }
          : tmdbTestState === "fail"
            ? { msg: "Key rejected by TMDB — check the value", fg: "var(--status-bad-text)", icon: X, spin: false }
            : { msg: "Key detected — not verified yet", fg: "var(--status-warn)", icon: Warning, spin: false };
  const m2 = testMsg(tests.indexer, indexerMode === "prowlarr" ? (prowlarrResultMsg ?? "Imported") : "Saved — Kosmos will search this indexer", "We'll save it to Settings → Indexers", failMsg.indexer);
  const m3 = testMsg(tests["download-client"], "Saved — Kosmos will send grabs here", "We'll save it to Settings → Download Clients", failMsg["download-client"]);

  const configuredSteps: StepKey[] = ["metadata", "indexer", "download-client", "media-folder"];
  const configured = configuredSteps.filter((k) => done[k]).length;
  const doneBlurb =
    configured === configuredSteps.length
      ? "Metadata, an indexer, a download client and a media folder are all wired up. Add something and Kosmos takes it from there."
      : configured === 0
        ? "Nothing configured yet — you can browse straight away, and set sources up whenever you like."
        : "That's enough to start. The steps you skipped are waiting under Settings, unchanged.";

  function sumRow(key: StepKey, label: string, value: string) {
    const ok = !!done[key];
    return { key, label, value: ok ? value : "skipped", ok };
  }

  const summary = [
    sumRow("metadata", "Metadata source", "TMDB · key detected"),
    sumRow("indexer", "Indexer", indexerMode === "prowlarr" ? "Prowlarr" : f2.name.trim() || "indexer"),
    sumRow("download-client", "Download client", `${f3.client} · ${f3.baseUrl || DEFAULT_BASE_URL_BY_LABEL[f3.client]}`),
    sumRow(
      "media-folder",
      "Root folders",
      rootFolders && rootFolders.length > 0 ? `${rootFolders.length} configured` : "root folder",
    ),
  ];

  function next() {
    go(Math.min(steps.length - 1, stepIndex + 1));
  }
  function skip() {
    setSkipped((s) => ({ ...s, [step]: true }));
    go(Math.min(steps.length - 1, stepIndex + 1));
  }
  function back() {
    go(Math.max(0, stepIndex - 1));
  }

  const nextDisabled = step === "account" && !done.account;

  const keyType = showKey ? "text" : "password";
  const KeyEye = showKey ? EyeSlash : Eye;

  return (
    <div className="setup-shell">
      <div className="setup-glow" />

      <div className="setup-topbar">
        <span className="setup-topbar-mark" />
        <span className="setup-topbar-word">Kosmos</span>
        <div style={{ flex: 1 }} />
        <span className="setup-topbar-count">{step === "done" ? "setup complete" : `step ${stepIndex + 1} of ${steps.length}`}</span>
      </div>

      <div className="setup-dots">
        {steps.map((key, i) => (
          <div
            key={key}
            onClick={() => visited[i] && go(i)}
            className="setup-dot"
            style={{
              flex: i === stepIndex ? 2.4 : 1,
              background: i === stepIndex ? "var(--accent-gradient)" : i < stepIndex ? "rgba(145,132,217,.4)" : "var(--border)",
              cursor: visited[i] ? "pointer" : "default",
            }}
          />
        ))}
      </div>
      <div className="setup-step-label">{LABELS[step]}</div>

      <div className={`setup-content k-scroll${step === "welcome" ? " centered" : ""}`}>
        {step === "welcome" && (
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
            <div className="setup-footnote">A few short steps. Everything is changeable later.</div>
          </div>
        )}

        {step === "account" && (
          <AccountStep
            login={login}
            onNativeDone={() => {
              setDone((d) => ({ ...d, account: true }));
              next();
            }}
            onJellyfinDone={(serverId) => {
              setJellyfinServerId(serverId);
              setDone((d) => ({ ...d, account: true }));
              go(stepIndex + 1);
            }}
          />
        )}

        {step === "jellyfin-libraries" && jellyfinServerId && (
          <JellyfinLibrariesStep
            serverId={jellyfinServerId}
            onDone={(note) => {
              setJellyfinFolderNote(note);
              reloadRootFolders();
              next();
            }}
          />
        )}

        {step === "metadata" && (
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
                <span className="setup-test-btn" onClick={tmdbConfigured ? testTmdbConnection : checkTmdb}>
                  <m1.icon size={13} className={m1.spin ? "spin" : ""} />
                  {tmdbConfigured && tmdbTestState === "untested" ? "Check" : "Re-check"}
                </span>
              </div>
              <p style={{ fontSize: 11.5, marginTop: 6, color: m1.fg }}>{m1.msg}</p>
            </div>
          </div>
        )}

        {step === "indexer" && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Add your first indexer.</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 18 }}>
              This is where Kosmos looks for releases. One is enough to start — add the rest later, or point it at
              Prowlarr and inherit them all.
            </p>

            <div className="setup-chip-row" style={{ marginBottom: 18 }}>
              <span
                className={`setup-chip${indexerMode === "manual" ? " active" : ""}`}
                onClick={() => !done.indexer && setIndexerMode("manual")}
              >
                Manual
              </span>
              <span
                className={`setup-chip${indexerMode === "prowlarr" ? " active" : ""}`}
                onClick={() => !done.indexer && setIndexerMode("prowlarr")}
              >
                <ArrowsClockwise size={14} />
                Prowlarr
              </span>
            </div>

            {indexerMode === "manual" && (
              <div style={{ display: "flex", flexDirection: "column", gap: 14, opacity: done.indexer ? 0.6 : 1 }}>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Name</label>
                  <input
                    className="input"
                    disabled={done.indexer}
                    value={f2.name}
                    onChange={(e) => {
                      setF2((s) => ({ ...s, name: e.target.value }));
                      setTests((t) => ({ ...t, indexer: undefined }));
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
                    disabled={done.indexer}
                    style={{ fontFamily: "var(--font-mono)" }}
                    value={f2.url}
                    onChange={(e) => {
                      setF2((s) => ({ ...s, url: e.target.value }));
                      setTests((t) => ({ ...t, indexer: undefined }));
                      setIndexerTestResult(null);
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
                      disabled={done.indexer}
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
                  {!done.indexer && (
                    <span className="setup-test-btn" onClick={testingIndexer ? undefined : testIndexerConnection}>
                      <PlugsConnected size={13} className={testingIndexer ? "spin" : ""} />
                      {testingIndexer ? "Testing…" : "Test connection"}
                    </span>
                  )}
                  <span
                    className="setup-test-btn"
                    onClick={done.indexer ? undefined : addIndexer}
                    style={done.indexer ? { cursor: "default", opacity: 0.7 } : undefined}
                  >
                    <m2.icon size={13} className={m2.spin ? "spin" : ""} />
                    {done.indexer ? "Added" : "Add indexer"}
                  </span>
                </div>
                {indexerTestResult && (
                  <p style={{ fontSize: 11.5, marginTop: 6, color: indexerTestResult.ok ? "var(--status-good-text)" : "var(--status-bad-text)" }}>
                    {indexerTestResult.msg}
                  </p>
                )}
                <p style={{ fontSize: 11.5, marginTop: 4, color: m2.fg }}>{m2.msg}</p>
              </div>
            )}

            {indexerMode === "prowlarr" && (
              <div style={{ display: "flex", flexDirection: "column", gap: 14, opacity: done.indexer ? 0.6 : 1 }}>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>
                    Prowlarr base URL
                  </label>
                  <input
                    className="input"
                    disabled={done.indexer}
                    style={{ fontFamily: "var(--font-mono)" }}
                    value={prowlarr.url}
                    onChange={(e) => {
                      setProwlarr((s) => ({ ...s, url: e.target.value }));
                      setTests((t) => ({ ...t, indexer: undefined }));
                      setProwlarrTestResult(null);
                    }}
                    placeholder="http://192.168.1.10:9696"
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>API key</label>
                  <div className="setup-key-field">
                    <Key size={15} color="var(--text-faint)" />
                    <input
                      type={keyType}
                      disabled={done.indexer}
                      value={prowlarr.key}
                      onChange={(e) => setProwlarr((s) => ({ ...s, key: e.target.value }))}
                      placeholder="Settings → General → API Key"
                    />
                    <span className="setup-key-toggle" onClick={() => setShowKey((v) => !v)}>
                      <KeyEye size={15} />
                    </span>
                  </div>
                </div>
                <div className="setup-test-row">
                  {!done.indexer && (
                    <span className="setup-test-btn" onClick={testingProwlarr ? undefined : testProwlarrConn}>
                      <PlugsConnected size={13} className={testingProwlarr ? "spin" : ""} />
                      {testingProwlarr ? "Testing…" : "Test connection"}
                    </span>
                  )}
                  <span
                    className="setup-test-btn"
                    onClick={done.indexer ? undefined : importProwlarr}
                    style={done.indexer ? { cursor: "default", opacity: 0.7 } : undefined}
                  >
                    <m2.icon size={13} className={m2.spin ? "spin" : ""} />
                    {done.indexer ? "Imported" : "Import from Prowlarr"}
                  </span>
                </div>
                {prowlarrTestResult && (
                  <p style={{ fontSize: 11.5, marginTop: 6, color: prowlarrTestResult.ok ? "var(--status-good-text)" : "var(--status-bad-text)" }}>
                    {prowlarrTestResult.msg}
                  </p>
                )}
                <p style={{ fontSize: 11.5, marginTop: 4, color: m2.fg }}>{m2.msg}</p>
              </div>
            )}
          </div>
        )}

        {step === "download-client" && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Who does the downloading?</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 18 }}>
              Kosmos hands releases to your existing client and watches the transfer. It never seeds or stores
              anything itself.
            </p>

            {CLIENTS.find((c) => c.label === f3.client)?.isTorrent && (
              <div
                style={{
                  display: "flex",
                  gap: 10,
                  alignItems: "flex-start",
                  padding: "12px 14px",
                  borderRadius: 10,
                  background: "rgba(238,152,145,.08)",
                  border: "1px solid rgba(238,152,145,.25)",
                  marginBottom: 18,
                }}
              >
                <Warning size={16} color="var(--status-bad-text)" style={{ flex: "none", marginTop: 1 }} />
                <p style={{ fontSize: 12, lineHeight: 1.6, color: "var(--text-secondary)" }}>
                  Torrent traffic is visible to your ISP and to anyone else in the swarm — your IP address is exposed
                  to every peer you exchange data with. Downloading copyrighted content this way can carry legal risk
                  depending on where you live, ranging from ISP warnings to rights-holder legal action. A VPN with
                  port forwarding hides your IP from the swarm; Usenet clients (SABnzbd, NZBGet) don't have this
                  exposure, since there's no peer swarm involved.
                </p>
              </div>
            )}

            <div className="setup-chip-row">
              {CLIENTS.map(({ label, icon: Icon }) => (
                <span
                  key={label}
                  className={`setup-chip${f3.client === label ? " active" : ""}`}
                  onClick={() => {
                    setF3((s) => ({ ...s, client: label, baseUrl: "" }));
                    setDownloadTestResult(null);
                  }}
                >
                  <Icon size={14} />
                  {label}
                </span>
              ))}
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 14, opacity: done["download-client"] ? 0.6 : 1 }}>
              <div>
                <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Base URL</label>
                <input
                  className="input"
                  disabled={done["download-client"]}
                  style={{ fontFamily: "var(--font-mono)" }}
                  value={f3.baseUrl}
                  onChange={(e) => {
                    setF3((s) => ({ ...s, baseUrl: e.target.value }));
                    setTests((t) => ({ ...t, "download-client": undefined }));
                    setDownloadTestResult(null);
                  }}
                  placeholder={DEFAULT_BASE_URL_BY_LABEL[f3.client]}
                />
              </div>
              <div className="setup-grid-2" style={{ gridTemplateColumns: "1fr 1fr" }}>
                <div>
                  <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Username</label>
                  <input
                    className="input"
                    disabled={done["download-client"]}
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
                      disabled={done["download-client"]}
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
              <div className="setup-test-row">
                {!done["download-client"] && (
                  <span className="setup-test-btn" onClick={testingConnection ? undefined : testDownloadClient}>
                    <PlugsConnected size={13} className={testingConnection ? "spin" : ""} />
                    {testingConnection ? "Testing…" : "Test connection"}
                  </span>
                )}
                <span
                  className="setup-test-btn"
                  onClick={done["download-client"] ? undefined : addDownloadClient}
                  style={done["download-client"] ? { cursor: "default", opacity: 0.7 } : undefined}
                >
                  <m3.icon size={13} className={m3.spin ? "spin" : ""} />
                  {done["download-client"] ? "Added" : "Add download client"}
                </span>
              </div>
              {downloadTestResult && (
                <p style={{ fontSize: 11.5, marginTop: 6, color: downloadTestResult.ok ? "var(--status-good-text)" : "var(--status-bad-text)" }}>
                  {downloadTestResult.msg}
                </p>
              )}
              <p style={{ fontSize: 11.5, marginTop: 4, color: m3.fg }}>{m3.msg}</p>
            </div>
          </div>
        )}

        {step === "media-folder" && (
          <div className="setup-step">
            <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Where does your library live?</h1>
            <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 22 }}>
              New grabs get organized under a root folder — <code style={{ fontFamily: "var(--font-mono)" }}>{"{title} ({year})"}</code>{" "}
              per title. Jellyfin sync only recognizes what Jellyfin already scanned, not new downloads — root
              folders are what those land in instead. Add more than one to split movies, shows and anime across
              different drives — pick which content types each accepts, or leave it open to accept anything.
            </p>

            {jellyfinFolderNote && (
              <p className="text-muted" style={{ marginBottom: 14 }}>
                {jellyfinFolderNote}
              </p>
            )}

            {rootFolders?.length === 0 && (
              <p className="text-muted" style={{ marginBottom: 14 }}>
                No root folders yet.
              </p>
            )}

            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 14 }}>
              {rootFolders?.map((folder) => (
                <div key={folder.id} className="setup-toggle-row">
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500, fontSize: 12.5, fontFamily: "var(--font-mono)" }}>{folder.path}</div>
                    <div className="text-faint" style={{ fontSize: 11, marginTop: 2 }}>
                      {folder.contentTypes.length === 0
                        ? "Accepts any content type"
                        : folder.contentTypes.map((t) => CONTENT_TYPES.find((c) => c.value === t)?.label ?? t).join(", ")}
                    </div>
                  </div>
                  <span
                    className="setup-test-btn"
                    onClick={removingFolderId === folder.id ? undefined : () => removeRootFolder(folder.id)}
                    style={{ flex: "none" }}
                  >
                    {removingFolderId === folder.id ? "Removing…" : "Remove"}
                  </span>
                </div>
              ))}
            </div>

            {folderError && (
              <p className="text-muted" style={{ marginBottom: 10 }}>
                {folderError}
              </p>
            )}

            <button type="button" className="btn btn-secondary" onClick={() => setBrowsingFolder(true)} disabled={savingFolder}>
              <FolderOpen size={15} />
              {savingFolder ? "Adding…" : "Add root folder"}
            </button>

            {browsingFolder && (
              <FolderBrowserModal onClose={() => setBrowsingFolder(false)} onSelect={addRootFolder} />
            )}
          </div>
        )}

        {step === "done" && (
          <div className="setup-step">
            <span style={{ display: "grid", placeItems: "center", width: 52, height: 52, borderRadius: 16, background: "rgba(79,191,139,.13)", border: "1px solid rgba(79,191,139,.3)", color: "var(--status-good-text)", marginBottom: 26 }}>
              <Check size={25} />
            </span>
            <h1 style={{ fontSize: 32, letterSpacing: "-0.03em", marginBottom: 13 }}>Kosmos is ready.</h1>
            <p className="text-muted" style={{ maxWidth: "46ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 26 }}>
              {doneBlurb}
            </p>

            <div className="setup-summary">
              {summary.map((row) => (
                <div key={row.key} className="setup-summary-row">
                  <span
                    className="setup-summary-icon"
                    style={{
                      background: row.ok ? "rgba(79,191,139,.16)" : "rgba(233,233,237,.07)",
                      color: row.ok ? "var(--status-good-text)" : "var(--text-faint)",
                    }}
                  >
                    {row.ok ? <Check size={12} /> : <Minus size={12} />}
                  </span>
                  <span style={{ flex: 1, minWidth: 0, fontWeight: 500, fontSize: 12.5, color: row.ok ? "var(--text)" : "var(--text-muted)" }}>
                    {row.label}
                  </span>
                  <span className="setup-summary-value">{row.value}</span>
                  {!row.ok && (
                    <span
                      style={{ fontSize: 11.5, fontWeight: 500, color: "var(--accent-2)", cursor: "pointer", flex: "none" }}
                      onClick={() => go(steps.indexOf(row.key))}
                    >
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
              <button type="button" className="btn btn-secondary" onClick={() => navigate("/library")}>
                <FolderPlus size={16} />
                Go to Library
              </button>
            </div>
          </div>
        )}
      </div>

      {step !== "done" && step !== "account" && step !== "jellyfin-libraries" && (
        <div className="setup-footer">
          {stepIndex > 0 ? (
            <button type="button" className="setup-back" onClick={back}>
              <ArrowLeft size={14} />
              Back
            </button>
          ) : (
            <span />
          )}
          <div style={{ flex: 1 }} />
          {stepIndex > 0 && (
            <button type="button" className="setup-skip" onClick={skip}>
              Skip for now
            </button>
          )}
          <button
            type="button"
            className="setup-next"
            onClick={next}
            disabled={nextDisabled}
            style={{
              background: nextDisabled ? "rgba(233,233,237,.09)" : "var(--accent-gradient)",
              color: nextDisabled ? "var(--text-faint)" : "#0b0c12",
            }}
          >
            {step === "welcome" ? "Get started" : step === "media-folder" ? "Finish" : "Continue"}
            <ArrowRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}

function AccountStep({
  login,
  onNativeDone,
  onJellyfinDone,
}: {
  login: (username: string, password: string) => Promise<void>;
  onNativeDone: () => void;
  onJellyfinDone: (serverId: string) => void;
}) {
  const [mode, setMode] = useState<"chooser" | "native" | "jellyfin">("chooser");

  if (mode === "chooser") {
    return (
      <div className="setup-step">
        <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Create your admin account.</h1>
        <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 26 }}>
          This is the very first account on this server — it becomes the admin.
        </p>
        <div style={{ display: "flex", flexDirection: "column", gap: 10, maxWidth: 360 }}>
          <button type="button" className="btn btn-hero" onClick={() => setMode("native")}>
            <LockSimple size={16} />
            Create a local admin account
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => setMode("jellyfin")}>
            <ArrowsClockwise size={16} />
            Connect Jellyfin instead
          </button>
        </div>
      </div>
    );
  }

  if (mode === "native") {
    return <NativeAccountForm onBack={() => setMode("chooser")} login={login} onDone={onNativeDone} />;
  }

  return <JellyfinAccountForm onBack={() => setMode("chooser")} login={login} onDone={onJellyfinDone} />;
}

function NativeAccountForm({
  onBack,
  login,
  onDone,
}: {
  onBack: () => void;
  login: (username: string, password: string) => Promise<void>;
  onDone: () => void;
}) {
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const passwordsMatch = confirmPassword.length === 0 || password === confirmPassword;
  const valid = username.trim().length > 0 && password.length > 0 && password === confirmPassword;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      await api.createUser({ username, displayName, password });
      await login(username, password);
      onDone();
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setError("An admin account already exists.");
      } else if (e instanceof ApiError && e.status === 400) {
        setError("That username is already taken.");
      } else {
        setError("Could not create the account.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="setup-step">
      <span className="setup-test-btn" style={{ marginBottom: 18, display: "inline-flex" }} onClick={onBack}>
        <ArrowLeft size={13} />
        Back
      </span>
      <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 20 }}>Create the admin account.</h1>
      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 14, maxWidth: 400 }}>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Username</label>
          <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Display name</label>
          <input
            className="input"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder={username || "defaults to username"}
          />
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Password</label>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Confirm password</label>
          <input
            className="input"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
          {!passwordsMatch && <p className="text-muted">Passwords don't match.</p>}
        </div>
        {error && <p className="text-muted">{error}</p>}
        <button type="submit" className="btn btn-hero" disabled={!valid || submitting} style={{ alignSelf: "flex-start" }}>
          {submitting ? "…" : "Create admin account"}
        </button>
      </form>
    </div>
  );
}

function JellyfinAccountForm({
  onBack,
  login,
  onDone,
}: {
  onBack: () => void;
  login: (username: string, password: string) => Promise<void>;
  onDone: (serverId: string) => void;
}) {
  const [serverUrl, setServerUrl] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid = serverUrl.trim().length > 0 && username.trim().length > 0 && password.length > 0;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      const { serverId } = await api.bootstrapJellyfin({ serverUrl: serverUrl.trim(), username, password });
      await login(username, password);
      onDone(serverId);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setError("An admin account already exists.");
      } else if (e instanceof ApiError) {
        setError(e.message || "Could not connect to that Jellyfin server.");
      } else {
        setError("Could not connect to that Jellyfin server.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="setup-step">
      <span className="setup-test-btn" style={{ marginBottom: 18, display: "inline-flex" }} onClick={onBack}>
        <ArrowLeft size={13} />
        Back
      </span>
      <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Connect Jellyfin.</h1>
      <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 20 }}>
        Sign in with a Jellyfin administrator account — it becomes the Kosmos admin too.
      </p>
      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 14, maxWidth: 400 }}>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>
            Jellyfin server URL
          </label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={serverUrl}
            onChange={(e) => setServerUrl(e.target.value)}
            placeholder="http://192.168.1.10:8096"
            autoFocus
            required
          />
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Username</label>
          <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11.5, fontWeight: 500, color: "var(--text-secondary)", marginBottom: 7 }}>Password</label>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        {error && <p className="text-muted">{error}</p>}
        <button type="submit" className="btn btn-hero" disabled={!valid || submitting} style={{ alignSelf: "flex-start" }}>
          {submitting ? "…" : "Connect"}
        </button>
      </form>
    </div>
  );
}

function JellyfinLibrariesStep({
  serverId,
  onDone,
}: {
  serverId: string;
  onDone: (note: string | null) => void;
}) {
  const [libraries, setLibraries] = useState<
    { id: string; name: string; collectionType: string | null; locations: string[] }[] | null
  >(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [continuing, setContinuing] = useState(false);

  useEffect(() => {
    api
      .listJellyfinLibraries(serverId)
      .then((libs) => {
        setLibraries(libs);
        setSelected(new Set(libs.map((l) => l.id)));
      })
      .catch((e) => setLoadError(e instanceof Error ? e.message : String(e)));
  }, [serverId]);

  function toggle(id: string) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const allSelected = !!libraries && selected.size === libraries.length;

  async function finish() {
    setContinuing(true);
    const libraryIds = allSelected ? [] : Array.from(selected);
    let note: string | null = null;
    try {
      await api.updateJellyfinLibrarySelection(serverId, libraryIds);
      // Pre-populates root folders from Jellyfin's own reported paths, so the media folder step
      // isn't asking for information Jellyfin already gave us. Runs server-side against Jellyfin
      // directly (not the possibly-stale `libraries` list here), and skips any collection type
      // with no folder equivalent (collections/boxsets are a database grouping, not a folder).
      const result = await api.autoRegisterRootFoldersFromJellyfin(serverId);
      if (result.registered > 0) {
        note = `Added ${result.registered} root folder${result.registered === 1 ? "" : "s"} from Jellyfin's own libraries.`;
      } else if (result.skipped > 0) {
        note =
          "Couldn't add root folders from Jellyfin automatically — its paths may not be visible from where Kosmos runs. Add them manually below.";
      }
    } catch {
      // Best-effort — onboarding still continues even if this step fails outright.
    } finally {
      // Fire-and-forget: a real library sync can take a while, and there's no reason to block
      // the rest of onboarding on it — it can be re-run from Settings → Jellyfin any time.
      api.syncJellyfinServer(serverId).catch(() => undefined);
      onDone(note);
    }
  }

  return (
    <div className="setup-step">
      <h1 style={{ fontSize: 30, letterSpacing: "-0.028em", marginBottom: 12 }}>Which libraries?</h1>
      <p className="text-muted" style={{ maxWidth: "50ch", fontSize: 14.5, lineHeight: 1.7, marginBottom: 22 }}>
        Pick what Kosmos should keep in sync with Jellyfin — you can change this later in Settings.
      </p>

      {loadError && <p className="text-muted">Could not load libraries: {loadError}</p>}
      {!loadError && !libraries && <p className="text-muted">Loading libraries…</p>}
      {libraries?.length === 0 && <p className="text-muted">No libraries found on this server.</p>}

      {libraries && libraries.length > 0 && (
        <>
          <div className="setup-test-row" style={{ marginBottom: 10 }}>
            <span className="setup-test-btn" onClick={() => setSelected(allSelected ? new Set() : new Set(libraries.map((l) => l.id)))}>
              {allSelected ? "Deselect all" : "Select all"}
            </span>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 8, maxWidth: 420, marginBottom: 26 }}>
            {libraries.map((library) => {
              const checked = selected.has(library.id);
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

      <button type="button" className="btn btn-hero" onClick={finish} disabled={continuing || !libraries}>
        {continuing ? "…" : "Continue"}
        <ArrowRight size={14} />
      </button>
    </div>
  );
}
