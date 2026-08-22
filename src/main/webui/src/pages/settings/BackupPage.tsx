import { ArchiveIcon as Archive, TrashIcon as Trash } from "@phosphor-icons/react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";

function formatSize(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function BackupPage() {
  const { data: backups, error: loadError, reload } = useApi(() => api.listBackups(), []);
  const [deletingFilename, setDeletingFilename] = useState<string | null>(null);

  async function remove(filename: string) {
    if (!window.confirm(`Delete ${filename}?`)) return;
    setDeletingFilename(filename);
    try {
      await api.deleteBackup(filename);
      reload();
    } finally {
      setDeletingFilename(null);
    }
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Backup</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Full database dumps via <code>pg_dump</code>. Scheduling and manual runs live on the{" "}
            <Link to="/settings/jobs">Jobs</Link> page under "Database Backup" — disabled by default until a
            "Run Now" confirms <code>pg_dump</code> works on this host. Restore isn't wired up yet; use{" "}
            <code>pg_restore</code> directly against a downloaded archive if you ever need one back.
          </p>
        </div>
      </div>

      {loadError && <p className="text-muted">Failed to load backups: {loadError}</p>}
      {backups?.length === 0 && <p className="text-muted">No backups yet.</p>}

      {backups?.map((file) => (
        <div key={file.filename} className="indexer-row">
          <span className="icon-tile">
            <Archive size={16} />
          </span>
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name" style={{ fontFamily: "var(--font-mono)" }}>
                {file.filename}
              </span>
            </div>
            <div className="indexer-row-sub">
              <span>{formatSize(file.sizeBytes)}</span>
              <span className="text-faint">·</span>
              <span>{new Date(file.createdAt).toLocaleString()}</span>
            </div>
          </div>
          <div className="indexer-row-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => remove(file.filename)}
              disabled={deletingFilename === file.filename}
            >
              <Trash size={14} />
              {deletingFilename === file.filename ? "Removing…" : "Remove"}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
