import { FolderOpenIcon as FolderOpen, PlusIcon as Plus, TrashIcon as Trash } from "@phosphor-icons/react";
import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { FolderBrowserModal } from "../../components/FolderBrowserModal";
import { useApi } from "../../hooks/useApi";
import type { LibraryContentType } from "../../api/types";

const CONTENT_TYPES: { value: LibraryContentType; label: string }[] = [
  { value: "movie", label: "Movies" },
  { value: "show", label: "Shows" },
  { value: "anime", label: "Anime" },
];

export default function RootFoldersPage() {
  const { data: folders, error: loadError, reload } = useApi(() => api.listRootFolders(), []);
  const [browsing, setBrowsing] = useState(false);
  const [pickedPath, setPickedPath] = useState<string | null>(null);
  const [pickedTypes, setPickedTypes] = useState<Set<LibraryContentType>>(new Set());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  function toggleType(type: LibraryContentType) {
    setPickedTypes((current) => {
      const next = new Set(current);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  }

  async function save() {
    if (!pickedPath) return;
    setSaving(true);
    setError(null);
    try {
      await api.createRootFolder(pickedPath, Array.from(pickedTypes));
      setPickedPath(null);
      setPickedTypes(new Set());
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not add this root folder");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    setDeletingId(id);
    try {
      await api.deleteRootFolder(id);
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not remove this root folder");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Root Folders</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Where grabbed releases get organized to. Add more than one to split movies, shows and anime across
            different drives or mounts — pick which content types each one accepts, or leave it open to accept
            anything.
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setBrowsing(true)}>
          <Plus size={15} weight="bold" />
          Add Root Folder
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load root folders: {loadError}</p>}
      {folders?.length === 0 && !pickedPath && <p className="text-muted">No root folders registered yet.</p>}

      {folders?.map((folder) => (
        <div key={folder.id} className="indexer-row">
          <span className="icon-tile">
            <FolderOpen size={16} />
          </span>
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name" style={{ fontFamily: "var(--font-mono)" }}>
                {folder.path}
              </span>
            </div>
            <div className="indexer-row-sub">
              {folder.contentTypes.length === 0
                ? "Accepts any content type"
                : folder.contentTypes.map((t) => CONTENT_TYPES.find((c) => c.value === t)?.label ?? t).join(", ")}
            </div>
          </div>
          <div className="indexer-row-actions">
            <button type="button" className="btn btn-secondary" onClick={() => remove(folder.id)} disabled={deletingId === folder.id}>
              <Trash size={14} />
              {deletingId === folder.id ? "Removing…" : "Remove"}
            </button>
          </div>
        </div>
      ))}

      {pickedPath && (
        <div className="setup-provider-card" style={{ marginTop: 14 }}>
          <p style={{ fontSize: 12.5, marginBottom: 10 }}>
            <span className="text-faint">Adding:</span>{" "}
            <span style={{ fontFamily: "var(--font-mono)" }}>{pickedPath}</span>
          </p>
          <div className="setup-chip-row" style={{ marginBottom: 12 }}>
            {CONTENT_TYPES.map(({ value, label }) => (
              <span
                key={value}
                className={`setup-chip${pickedTypes.has(value) ? " active" : ""}`}
                onClick={() => toggleType(value)}
              >
                {label}
              </span>
            ))}
          </div>
          <p className="text-faint" style={{ fontSize: 11.5, marginBottom: 12 }}>
            Leave all unchecked to accept any content type.
          </p>
          {error && <p className="text-muted">{error}</p>}
          <div className="dialog-footer" style={{ padding: 0, border: "none" }}>
            <button type="button" className="btn btn-secondary" onClick={() => setPickedPath(null)}>
              Cancel
            </button>
            <button type="button" className="btn btn-hero" onClick={save} disabled={saving}>
              {saving ? "Adding…" : "Add"}
            </button>
          </div>
        </div>
      )}

      {browsing && (
        <FolderBrowserModal
          onClose={() => setBrowsing(false)}
          onSelect={(path) => {
            setBrowsing(false);
            setPickedPath(path);
            setPickedTypes(new Set());
            setError(null);
          }}
        />
      )}
    </div>
  );
}
