import { FolderOpenIcon as FolderOpen, PlusIcon as Plus, TrashIcon as Trash } from "@phosphor-icons/react";
import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { FolderBrowserModal } from "../../components/FolderBrowserModal";
import { useApi } from "../../hooks/useApi";
import type { LibraryContentType } from "../../api/types";

const CONTENT_TYPE_LABELS: Record<LibraryContentType, string> = {
  movie: "Movies",
  show: "Shows",
  anime: "Anime",
};

export default function RootFoldersPage() {
  const { data: folders, error: loadError, reload } = useApi(() => api.listRootFolders(), []);
  const [browsing, setBrowsing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  async function add(path: string, contentTypes: LibraryContentType[]) {
    setBrowsing(false);
    setSaving(true);
    setError(null);
    try {
      await api.createRootFolder(path, contentTypes);
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
        <button type="button" className="btn btn-hero" onClick={() => setBrowsing(true)} disabled={saving}>
          <Plus size={15} weight="bold" />
          {saving ? "Adding…" : "Add Root Folder"}
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load root folders: {loadError}</p>}
      {error && <p className="text-muted">{error}</p>}
      {folders?.length === 0 && <p className="text-muted">No root folders registered yet.</p>}

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
                : folder.contentTypes.map((t) => CONTENT_TYPE_LABELS[t]).join(", ")}
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

      {browsing && <FolderBrowserModal onClose={() => setBrowsing(false)} onSelect={add} />}
    </div>
  );
}
