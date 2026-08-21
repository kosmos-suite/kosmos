import { ArrowLeftIcon as ArrowLeft, FolderIcon as Folder, FolderOpenIcon as FolderOpen, XIcon as X } from "@phosphor-icons/react";
import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { BrowseResult, LibraryContentType } from "../api/types";

const CONTENT_TYPES: { value: LibraryContentType; label: string }[] = [
  { value: "movie", label: "Movies" },
  { value: "show", label: "Shows" },
  { value: "anime", label: "Anime" },
];

/** Browses the server's own filesystem and, in the same dialog, picks which content types the
 * chosen folder accepts — used for adding a library root folder. */
export function FolderBrowserModal({
  initialPath,
  onClose,
  onSelect,
}: {
  initialPath?: string;
  onClose: () => void;
  onSelect: (path: string, contentTypes: LibraryContentType[]) => void;
}) {
  const [current, setCurrent] = useState<BrowseResult | null>(null);
  const [pathInput, setPathInput] = useState(initialPath ?? "");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [contentTypes, setContentTypes] = useState<Set<LibraryContentType>>(new Set());

  async function load(path?: string) {
    setLoading(true);
    setError(null);
    try {
      const result = await api.browseFilesystem(path);
      setCurrent(result);
      setPathInput(result.path);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load(initialPath);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function toggleType(type: LibraryContentType) {
    setContentTypes((current) => {
      const next = new Set(current);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span className="icon-tile">
            <FolderOpen size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add a root folder</div>
            <div className="dialog-sub">Browsing the server's own filesystem.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Path</label>
          <input
            className="input"
            style={{ fontFamily: "var(--font-mono)" }}
            value={pathInput}
            onChange={(e) => setPathInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") load(pathInput);
            }}
          />
        </div>

        {error && <p className="text-muted">{error}</p>}
        {loading && <p className="text-muted">Loading…</p>}

        {current && !loading && (
          <div style={{ maxHeight: 220, overflowY: "auto", display: "flex", flexDirection: "column", gap: 4, marginBottom: 14 }}>
            {current.parentPath && (
              <div className="setup-toggle-row" style={{ cursor: "pointer" }} onClick={() => load(current.parentPath!)}>
                <ArrowLeft size={14} style={{ marginRight: 8 }} />
                ..
              </div>
            )}
            {current.directories.map((dir) => (
              <div key={dir.path} className="setup-toggle-row" style={{ cursor: "pointer" }} onClick={() => load(dir.path)}>
                <Folder size={14} style={{ marginRight: 8 }} />
                {dir.name}
              </div>
            ))}
            {current.directories.length === 0 && <p className="text-faint">No subfolders here.</p>}
          </div>
        )}

        <div className="field">
          <label>Content types</label>
          <div className="setup-chip-row">
            {CONTENT_TYPES.map(({ value, label }) => (
              <span
                key={value}
                className={`setup-chip${contentTypes.has(value) ? " active" : ""}`}
                onClick={() => toggleType(value)}
              >
                {label}
              </span>
            ))}
          </div>
          <p className="text-faint" style={{ fontSize: 11.5, marginTop: 7 }}>
            Leave all unchecked to accept any content type.
          </p>
        </div>

        <div className="dialog-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-hero"
            onClick={() => current && onSelect(current.path, Array.from(contentTypes))}
            disabled={!current}
          >
            Add root folder
          </button>
        </div>
      </div>
    </div>
  );
}
