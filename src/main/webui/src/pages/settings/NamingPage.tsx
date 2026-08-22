import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import type { NamingSettings } from "../../api/types";

const CONTENT_TYPE_LABELS: Record<NamingSettings["contentType"], string> = {
  movie: "Movies",
  show: "Shows",
  anime: "Anime",
};

const TOKENS: { token: string; description: string }[] = [
  { token: "{Title}", description: "Movie title, or the owning show/anime's title" },
  { token: "{Year}", description: "Release year" },
  { token: "{EpisodeTitle}", description: "The episode's own title (shows/anime only)" },
  { token: "{Season}", description: "Season number (shows only) — pad with e.g. {Season:00}" },
  { token: "{Episode}", description: "Episode number within its season (shows only)" },
  { token: "{Absolute}", description: "Episode number continuous across the whole anime franchise" },
];

interface FormState {
  folderTemplate: string;
  fileTemplate: string;
  seasonFolderTemplate: string;
}

function toForm(settings: NamingSettings): FormState {
  return {
    folderTemplate: settings.folderTemplate,
    fileTemplate: settings.fileTemplate,
    seasonFolderTemplate: settings.seasonFolderTemplate ?? "",
  };
}

function ContentTypeCard({ settings, onSaved }: { settings: NamingSettings; onSaved: () => void }) {
  const [form, setForm] = useState<FormState>(toForm(settings));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dirty =
    form.folderTemplate !== settings.folderTemplate ||
    form.fileTemplate !== settings.fileTemplate ||
    form.seasonFolderTemplate !== (settings.seasonFolderTemplate ?? "");

  async function save() {
    setSaving(true);
    setError(null);
    try {
      await api.updateNamingSettings(settings.contentType, {
        folderTemplate: form.folderTemplate,
        fileTemplate: form.fileTemplate,
        seasonFolderTemplate: settings.contentType === "show" ? form.seasonFolderTemplate : null,
      });
      onSaved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not save naming settings");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="indexer-row" style={{ flexDirection: "column", alignItems: "stretch", gap: 12 }}>
      <div className="indexer-row-title-line">
        <span className="indexer-row-name">{CONTENT_TYPE_LABELS[settings.contentType]}</span>
      </div>

      <div className="field">
        <label>Folder</label>
        <input
          className="input"
          style={{ fontFamily: "var(--font-mono)" }}
          value={form.folderTemplate}
          onChange={(e) => setForm((f) => ({ ...f, folderTemplate: e.target.value }))}
        />
      </div>

      {settings.contentType === "show" && (
        <div className="field">
          <label>Season folder</label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={form.seasonFolderTemplate}
            onChange={(e) => setForm((f) => ({ ...f, seasonFolderTemplate: e.target.value }))}
          />
        </div>
      )}

      <div className="field">
        <label>File name</label>
        <input
          className="input"
          style={{ fontFamily: "var(--font-mono)" }}
          value={form.fileTemplate}
          onChange={(e) => setForm((f) => ({ ...f, fileTemplate: e.target.value }))}
        />
      </div>

      {error && <p className="text-muted">{error}</p>}

      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <button type="button" className="btn btn-primary" onClick={save} disabled={!dirty || saving}>
          {saving ? "Saving…" : "Save"}
        </button>
      </div>
    </div>
  );
}

export default function NamingPage() {
  const { data: settings, error: loadError, reload } = useApi(() => api.listNamingSettings(), []);

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Naming</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            How Kosmos names the folders and files it writes on import. Nothing here is required — every content
            type already has a sensible default.
          </p>
        </div>
      </div>

      {loadError && <p className="text-muted">Failed to load naming settings: {loadError}</p>}

      {settings?.map((s) => (
        <ContentTypeCard key={s.contentType} settings={s} onSaved={reload} />
      ))}

      <div className="indexer-row" style={{ flexDirection: "column", alignItems: "stretch", gap: 8 }}>
        <div className="indexer-row-title-line">
          <span className="indexer-row-name">Tokens</span>
        </div>
        {TOKENS.map(({ token, description }) => (
          <div key={token} style={{ display: "flex", gap: 12 }}>
            <span className="text-muted" style={{ fontFamily: "var(--font-mono)", minWidth: 140 }}>
              {token}
            </span>
            <span className="text-muted">{description}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
