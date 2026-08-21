import {
  ArrowsClockwiseIcon as ArrowsClockwise,
  BellIcon as Bell,
  CheckIcon as Check,
  CheckCircleIcon as CheckCircle,
  CircleNotchIcon as CircleNotch,
  DownloadSimpleIcon as DownloadSimple,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  KeyIcon as Key,
  LinkIcon,
  MagnifyingGlassIcon as MagnifyingGlass,
  PuzzlePieceIcon as PuzzlePiece,
  ShieldCheckIcon as ShieldCheck,
  SlidersHorizontalIcon as SlidersHorizontal,
  StorefrontIcon as Storefront,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState } from "react";
import { api } from "../../api/client";
import type { PluginManifest, RegistryEntry } from "../../api/types";
import { Toggle } from "../../components/Toggle";
import {
  installedPlugins as seedInstalled,
  registryPlugins as seedRegistry,
  type InstalledPlugin,
  type RegistryPlugin,
} from "../../mocks/mockPlugins";

const HUE_BY_KIND: Record<string, InstalledPlugin["hue"]> = { metadata: "violet" };

function manifestToInstalledPlugin(manifest: PluginManifest): InstalledPlugin {
  return {
    id: manifest.slug,
    name: manifest.name,
    version: manifest.version,
    publisher: "Kosmos core",
    hue: HUE_BY_KIND[manifest.kind] ?? "grey",
    description: `Real ${manifest.kind} plugin running through Kosmos's WASM host, not a hardcoded class.`,
    capabilities: [manifest.kind],
    enabled: true,
    statusKind: "good",
    statusText: "loaded through the WASM plugin host",
    apiKeyConfigurable: false,
    allowedHosts: manifest.permissions.allowedHosts,
  };
}

function registryEntryToRegistryPlugin(entry: RegistryEntry): RegistryPlugin {
  return {
    id: entry.slug,
    name: entry.name,
    kind: entry.category,
    publisher: "Community",
    category: entry.category,
    description: entry.description,
    comingSoon: false,
    real: true,
  };
}

const HUE_BG: Record<InstalledPlugin["hue"], string> = {
  violet: "rgba(145,132,217,.16)",
  cyan: "rgba(90,200,220,.16)",
  amber: "rgba(224,169,74,.16)",
  grey: "rgba(233,233,237,.1)",
};
const HUE_FG: Record<InstalledPlugin["hue"], string> = {
  violet: "#d2cefd",
  cyan: "#a6e6f0",
  amber: "#ebc182",
  grey: "var(--text-secondary)",
};

const REGISTRY_FILTERS = ["All", "Metadata", "Artwork", "Subtitles", "Coming soon"] as const;

function monogram(name: string): string {
  const words = name.split(/[\s.]+/).filter(Boolean);
  if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

export default function PluginsPage() {
  const [installed, setInstalled] = useState<InstalledPlugin[]>(seedInstalled);
  const [registry, setRegistry] = useState<RegistryPlugin[]>(seedRegistry);
  const [notified, setNotified] = useState<Record<string, boolean>>({});
  const [installing, setInstalling] = useState<Record<string, boolean>>({});
  const [checking, setChecking] = useState(false);
  const [checkedOnce, setCheckedOnce] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<(typeof REGISTRY_FILTERS)[number]>("All");
  const [configuring, setConfiguring] = useState<InstalledPlugin | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .listInstalledPlugins()
      .then((manifests) => {
        if (cancelled) return;
        const real = manifests.map(manifestToInstalledPlugin);
        setInstalled((list) => [...real, ...list.filter((p) => !real.some((r) => r.id === p.id))]);
      })
      .catch(() => {
        // Plugin host is best-effort here — the rest of the page still works on sample data.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    api
      .listRegistryPlugins()
      .then((entries) => {
        if (cancelled) return;
        const real = entries.map(registryEntryToRegistryPlugin);
        setRegistry((list) => [...real, ...list.filter((p) => !real.some((r) => r.id === p.id))]);
      })
      .catch(() => {
        // Registry fetch is best-effort here too — the rest of the page still works on sample data.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast((c) => (c === message ? null : c)), 3600);
  }

  const visibleRegistry = useMemo(() => {
    const q = query.trim().toLowerCase();
    return registry.filter((p) => {
      if (filter === "Coming soon" && !p.comingSoon) return false;
      if (filter !== "All" && filter !== "Coming soon" && p.category !== filter) return false;
      if (q && !p.name.toLowerCase().includes(q) && !p.description.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [registry, query, filter]);

  function install(plugin: RegistryPlugin) {
    if (plugin.real) {
      showToast("Real installs aren't wired up yet — this entry is genuinely in the registry, though.");
      return;
    }
    setInstalling((s) => ({ ...s, [plugin.id]: true }));
    window.setTimeout(() => {
      setInstalling((s) => ({ ...s, [plugin.id]: false }));
      setRegistry((r) => r.filter((p) => p.id !== plugin.id));
      setInstalled((list) => [
        ...list,
        {
          id: plugin.id,
          name: plugin.name,
          version: "1.0.0",
          publisher: plugin.publisher,
          hue: "grey",
          description: plugin.description,
          capabilities: [plugin.kind],
          enabled: true,
          statusKind: "neutral",
          statusText: "just installed — nothing synced yet",
          apiKeyConfigurable: true,
        },
      ]);
      showToast(`${plugin.name} installed`);
    }, 1700);
  }

  const enabledCount = installed.filter((p) => p.enabled).length;

  return (
    <div>
      <div className="plugin-hero">
        <span className="hero-kicker">
          <PuzzlePiece size={12} weight="bold" />
          Extensible by design
        </span>
        <h1 className="plugin-hero-title">Plugins</h1>
        <p className="plugin-hero-desc">
          Metadata, artwork, numbering and subtitles all arrive through plugins — swap a source
          without touching a single file on disk. Anime can resolve through AniList while movies
          stay on TMDB.
        </p>
        <div className="plugin-hero-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setChecking(true);
              window.setTimeout(() => {
                setChecking(false);
                setCheckedOnce(true);
              }, 1200);
            }}
          >
            {checking ? <ArrowsClockwise size={14} className="spin" /> : <ArrowsClockwise size={14} />}
            {checking ? "Checking…" : checkedOnce ? "Up to date" : "Check for updates"}
          </button>
          <button
            type="button"
            className="btn btn-hero"
            onClick={() => document.getElementById("plugin-registry")?.scrollIntoView({ behavior: "smooth" })}
          >
            <Storefront size={15} />
            Browse Plugins
          </button>
        </div>
        <div className="plugin-hero-stats">
          <div className="stat-card">
            <div className="stat-card-value">
              {enabledCount} of {installed.length}
            </div>
            <div className="stat-card-label">Plugins enabled</div>
          </div>
          <div className="stat-card">
            <div className="stat-card-value">4,298</div>
            <div className="stat-card-label">Titles resolved</div>
          </div>
          <div className="stat-card">
            <div className="stat-card-value">{registry.length}</div>
            <div className="stat-card-label">In the registry</div>
          </div>
          <div className="stat-card">
            <div className="stat-card-value">2h ago</div>
            <div className="stat-card-label">Last registry sync</div>
          </div>
        </div>
      </div>

      <div>
        <p className="text-muted" style={{ marginTop: 0, marginBottom: 18 }}>
          The Chicory-based WASM plugin host is real — the Installed section merges in plugins
          loaded live from <code>GET /plugins</code>, and the Registry section below merges in
          entries fetched live from{" "}
          <a href="https://github.com/kosmos-suite/kosmos-plugin-registry" target="_blank" rel="noreferrer">
            kosmos-plugin-registry
          </a>
          . Everything else here (TMDB/AniList as hardcoded Java classes, and actually installing
          a registry plugin) is still design-phase sample data.
        </p>

        <div className="content-row-header" style={{ padding: "0 0 14px" }}>
          <h2 style={{ fontSize: 15 }}>Installed</h2>
          <span className="content-row-sub">
            {installed.length} installed · {enabledCount} active
          </span>
        </div>

        <div className="plugin-grid">
          {installed.map((plugin) => (
            <div key={plugin.id} className="plugin-card">
              <div className="plugin-card-top">
                <span
                  className="icon-tile"
                  style={{ background: HUE_BG[plugin.hue], color: HUE_FG[plugin.hue], borderColor: "transparent" }}
                >
                  {monogram(plugin.name)}
                </span>
                <div className="plugin-card-titles">
                  <div className="plugin-card-title-line">
                    <span className="plugin-card-name">{plugin.name}</span>
                    <span className="plugin-card-version">v{plugin.version}</span>
                  </div>
                  <div className="plugin-card-desc" style={{ marginBottom: 0 }}>
                    {plugin.publisher}
                  </div>
                </div>
                <Toggle
                  on={plugin.enabled}
                  onChange={(next) =>
                    setInstalled((list) => list.map((p) => (p.id === plugin.id ? { ...p, enabled: next } : p)))
                  }
                  label={`Enable ${plugin.name}`}
                />
              </div>
              <div className="plugin-card-desc">{plugin.description}</div>
              <div className="plugin-card-caps">
                {plugin.capabilities.map((cap) => (
                  <span key={cap} className="tag tag-neutral">
                    {cap}
                  </span>
                ))}
              </div>
              {plugin.allowedHosts && (
                <div className="plugin-card-desc" style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <ShieldCheck size={13} />
                  Can reach: {plugin.allowedHosts.length ? plugin.allowedHosts.join(", ") : "nothing (no hosts allowed)"}
                </div>
              )}
              <div className="plugin-card-divider" />
              <div className="plugin-card-status">
                <span
                  className="dot"
                  style={{
                    background: plugin.enabled
                      ? plugin.statusKind === "good"
                        ? "var(--status-good)"
                        : plugin.statusKind === "warn"
                          ? "var(--status-warn)"
                          : "var(--text-disabled)"
                      : "var(--text-disabled)",
                  }}
                />
                {plugin.enabled ? plugin.statusText : "disabled — nothing will resolve through it"}
              </div>
              <button
                type="button"
                className="btn btn-secondary btn-block"
                style={{ marginTop: 12 }}
                onClick={() => setConfiguring(plugin)}
              >
                <SlidersHorizontal size={14} />
                Configure
              </button>
            </div>
          ))}
        </div>

        <div id="plugin-registry" className="content-row-header" style={{ padding: "34px 0 4px" }}>
          <h2 style={{ fontSize: 15 }}>Registry</h2>
          <span className="content-row-sub">signed plugins from the Kosmos index</span>
        </div>

        <div className="registry-toolbar" style={{ marginTop: 14 }}>
          <div className="registry-search">
            <MagnifyingGlass size={14} />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search registry…" />
          </div>
          <div className="filter-tabs" style={{ margin: 0 }}>
            {REGISTRY_FILTERS.map((f) => (
              <button key={f} type="button" className={filter === f ? "active" : ""} onClick={() => setFilter(f)}>
                {f}
              </button>
            ))}
          </div>
          <button
            type="button"
            className="btn"
            style={{
              border: "1px dashed var(--border-strong)",
              color: "var(--text-muted)",
              marginLeft: "auto",
            }}
            onClick={() => showToast("Install from URL isn't wired up yet")}
          >
            <LinkIcon size={14} />
            Install from URL
          </button>
        </div>

        <div className="registry-grid">
          {visibleRegistry.map((plugin) => (
            <div key={plugin.id} className={`registry-card${plugin.comingSoon ? " soon" : ""}`}>
              <div className="registry-card-top">
                <span className="icon-tile" style={{ width: 36, height: 36 }}>
                  {monogram(plugin.name)}
                </span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="plugin-card-title-line">
                    <span className="plugin-card-name" style={{ fontSize: 13.5 }}>
                      {plugin.name}
                    </span>
                    {plugin.comingSoon ? (
                      <span className="tag tag-neutral">Coming soon</span>
                    ) : (
                      <span className="tag tag-outline">{plugin.kind}</span>
                    )}
                    {plugin.real && (
                      <span className="tag tag-neutral" title="Fetched live from kosmos-plugin-registry">
                        From registry
                      </span>
                    )}
                  </div>
                  <div className="registry-card-author">
                    {plugin.publisher}
                    {plugin.comingSoon && plugin.reviewNote ? ` · ${plugin.reviewNote}` : ""}
                  </div>
                </div>
              </div>
              <div className="registry-card-desc">{plugin.description}</div>
              {plugin.comingSoon ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-block"
                  disabled={!!notified[plugin.id]}
                  onClick={() => setNotified((s) => ({ ...s, [plugin.id]: true }))}
                >
                  {notified[plugin.id] ? (
                    <>
                      <Check size={14} />
                      We'll let you know
                    </>
                  ) : (
                    <>
                      <Bell size={14} />
                      Notify me
                    </>
                  )}
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-secondary btn-block"
                  disabled={!!installing[plugin.id]}
                  onClick={() => install(plugin)}
                >
                  {installing[plugin.id] ? (
                    <>
                      <CircleNotch size={14} className="spin" />
                      Installing…
                    </>
                  ) : (
                    <>
                      <DownloadSimple size={14} />
                      Install
                    </>
                  )}
                </button>
              )}
            </div>
          ))}
        </div>

        <div className="info-banner">
          <ShieldCheck size={16} />
          Registry plugins are signature-checked and run sandboxed — no filesystem access beyond
          the library paths you grant.{" "}
          <a href="#" onClick={(e) => e.preventDefault()}>
            Read the plugin contract.
          </a>
        </div>
      </div>

      {configuring && (
        <ConfigureModal plugin={configuring} onClose={() => setConfiguring(null)} onSave={() => showToast("Saved")} />
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

const LANGUAGES = ["English", "Japanese", "Original", "German"];

function ConfigureModal({
  plugin,
  onClose,
  onSave,
}: {
  plugin: InstalledPlugin;
  onClose: () => void;
  onSave: () => void;
}) {
  const [apiKey, setApiKey] = useState("");
  const [showKey, setShowKey] = useState(false);
  const [language, setLanguage] = useState("English");
  const [preferOriginal, setPreferOriginal] = useState(true);
  const [fallback, setFallback] = useState(true);
  const [includeAdult, setIncludeAdult] = useState(false);
  const [testState, setTestState] = useState<"idle" | "testing" | "ok">("idle");

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span
            className="icon-tile"
            style={{ background: HUE_BG[plugin.hue], color: HUE_FG[plugin.hue], borderColor: "transparent" }}
          >
            {monogram(plugin.name)}
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">{plugin.name}</div>
            <div className="dialog-sub">
              v{plugin.version} · by {plugin.publisher}
            </div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        {plugin.allowedHosts && (
          <div className="field">
            <label>Permissions</label>
            <div className="plugin-card-desc" style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <ShieldCheck size={13} />
              {plugin.allowedHosts.length
                ? `Can reach: ${plugin.allowedHosts.join(", ")}`
                : "Can't reach any host — no permissions granted."}
            </div>
            <div className="settings-row-sub" style={{ marginTop: 6 }}>
              Enforced by the plugin host itself — every outbound call this plugin makes is checked
              against this list before it leaves the process. No other configuration exists for it
              yet.
            </div>
          </div>
        )}

        {!plugin.allowedHosts && plugin.apiKeyConfigurable && (
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
                style={{ position: "absolute", right: 10, top: 10, background: "transparent", border: 0, color: "var(--text-faint)", cursor: "pointer" }}
              >
                {showKey ? <EyeSlash size={15} /> : <Eye size={15} />}
              </button>
            </div>
          </div>
        )}

        {!plugin.allowedHosts && (
          <>
            <div className="field">
              <label>Metadata language</label>
              <div className="pill-picker">
                {LANGUAGES.map((lang) => (
                  <button key={lang} type="button" className={lang === language ? "active" : ""} onClick={() => setLanguage(lang)}>
                    {lang}
                  </button>
                ))}
              </div>
            </div>

            <div className="settings-row">
              <div className="settings-row-main">
                <div className="settings-row-title">Prefer original titles</div>
                <div className="settings-row-sub">Show “Sousou no Frieren” rather than the English release title.</div>
              </div>
              <Toggle on={preferOriginal} onChange={setPreferOriginal} label="Prefer original titles" />
            </div>
            <div className="settings-row">
              <div className="settings-row-main">
                <div className="settings-row-title">Fall back to other plugins</div>
                <div className="settings-row-sub">If this source has no match, try the next enabled plugin in order.</div>
              </div>
              <Toggle on={fallback} onChange={setFallback} label="Fall back to other plugins" />
            </div>
            <div className="settings-row">
              <div className="settings-row-main">
                <div className="settings-row-title">Include adult results</div>
                <div className="settings-row-sub">Off by default; affects search and discovery rows.</div>
              </div>
              <Toggle on={includeAdult} onChange={setIncludeAdult} label="Include adult results" />
            </div>
          </>
        )}

        <div className="dialog-footer">
          <span className="dialog-hint">
            {plugin.allowedHosts
              ? "Nothing here is editable yet."
              : testState === "testing"
                ? "Testing…"
                : testState === "ok"
                  ? "Connection looks good."
                  : "Configuration isn't sent anywhere yet."}
          </span>
          {plugin.allowedHosts ? (
            <button type="button" className="btn btn-hero" onClick={onClose}>
              Close
            </button>
          ) : (
            <>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setTestState("testing");
                  window.setTimeout(() => setTestState("ok"), 800);
                }}
              >
                Test
              </button>
              <button
                type="button"
                className="btn btn-hero"
                onClick={() => {
                  onSave();
                  onClose();
                }}
              >
                Save
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
