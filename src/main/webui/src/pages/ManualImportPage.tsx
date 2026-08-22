import { useState } from "react";
import { CheckIcon as Check, MagnifyingGlassIcon as MagnifyingGlass, WarningIcon as Warning, XIcon as X } from "@phosphor-icons/react";
import { api, ApiError } from "../api/client";
import type { CommitImportResult, ImportCandidate } from "../api/types";

const CONTENT_TYPE_LABEL: Record<string, string> = {
  movie: "Movie",
  episode: "Episode",
  anime_episode: "Anime Episode",
};

function fmtGb(bytes: number): string {
  return `${(bytes / 1024 ** 3).toFixed(2)} GB`;
}

function candidateLabel(c: ImportCandidate): string {
  if (c.suggestedContentType === "movie") return c.suggestedMediaItemTitle ?? "";
  if (c.seasonNumber != null && c.episodeNumber != null) {
    return `${c.suggestedMediaItemTitle} — S${String(c.seasonNumber).padStart(2, "0")}E${String(c.episodeNumber).padStart(2, "0")}`;
  }
  if (c.absoluteEpisodeNumber != null) {
    return `${c.suggestedMediaItemTitle} — ${c.absoluteEpisodeNumber}`;
  }
  return c.suggestedMediaItemTitle ?? "";
}

export default function ManualImportPage() {
  const [sourcePath, setSourcePath] = useState("");
  const [scanning, setScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);
  const [candidates, setCandidates] = useState<ImportCandidate[] | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [committing, setCommitting] = useState(false);
  const [results, setResults] = useState<Map<string, CommitImportResult>>(new Map());

  async function scan() {
    if (!sourcePath.trim()) return;
    setScanning(true);
    setScanError(null);
    setResults(new Map());
    try {
      const found = await api.scanImportPath(sourcePath.trim());
      setCandidates(found);
      setSelected(new Set(found.filter((c) => c.suggestedMediaItemId).map((c) => c.sourcePath)));
    } catch (e) {
      setScanError(e instanceof ApiError ? e.message : "Could not scan this path");
      setCandidates(null);
    } finally {
      setScanning(false);
    }
  }

  function toggle(path: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(path)) next.delete(path);
      else next.add(path);
      return next;
    });
  }

  async function commit() {
    if (!candidates) return;
    const items = candidates
      .filter((c) => selected.has(c.sourcePath) && c.suggestedMediaItemId)
      .map((c) => ({ sourcePath: c.sourcePath, mediaItemId: c.suggestedMediaItemId! }));
    if (items.length === 0) return;
    setCommitting(true);
    try {
      const outcomes = await api.commitImport(items);
      setResults(new Map(outcomes.map((o) => [o.sourcePath, o])));
    } catch (e) {
      setScanError(e instanceof ApiError ? e.message : "Import failed");
    } finally {
      setCommitting(false);
    }
  }

  const selectableCount = candidates?.filter((c) => c.suggestedMediaItemId).length ?? 0;

  return (
    <div className="page with-top-padding">
      <div className="page-header">
        <h1>Manual Import</h1>
      </div>
      <p className="text-muted" style={{ maxWidth: "70ch", marginBottom: 20 }}>
        Point at a folder Kosmos can read — a download client's output directory, most commonly — and review every
        video file it found before importing. Only files with a suggested match can be committed; anything left
        unmatched needs the quick-attach import from that title's own page instead.
      </p>

      <div style={{ display: "flex", gap: 10, marginBottom: 20 }}>
        <input
          className="input"
          style={{ fontFamily: "var(--font-mono)" }}
          placeholder="/downloads/some-release"
          value={sourcePath}
          onChange={(e) => setSourcePath(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && scan()}
        />
        <button type="button" className="btn btn-primary" onClick={scan} disabled={scanning || !sourcePath.trim()}>
          <MagnifyingGlass size={14} />
          {scanning ? "Scanning…" : "Scan"}
        </button>
      </div>

      {scanError && <p className="text-muted">{scanError}</p>}

      {candidates && candidates.length === 0 && <p className="text-muted">No importable video files found under that path.</p>}

      {candidates && candidates.length > 0 && (
        <>
          {candidates.map((c) => {
            const result = results.get(c.sourcePath);
            const matched = !!c.suggestedMediaItemId;
            return (
              <div key={c.sourcePath} className="indexer-row">
                <input
                  type="checkbox"
                  checked={selected.has(c.sourcePath)}
                  disabled={!matched}
                  onChange={() => toggle(c.sourcePath)}
                  style={{ marginTop: 4 }}
                />
                <div className="indexer-row-main">
                  <div className="indexer-row-title-line">
                    <span className="indexer-row-name" style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>
                      {c.sourcePath}
                    </span>
                  </div>
                  <div className="indexer-row-sub" style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <span>{fmtGb(c.sizeBytes)}</span>
                    <span className="sep" />
                    {matched ? (
                      <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                        {c.ambiguous && <Warning size={13} color="var(--status-warn)" />}
                        {CONTENT_TYPE_LABEL[c.suggestedContentType ?? ""]}: {candidateLabel(c)}
                        {c.ambiguous && " (multiple titles matched — double-check this one)"}
                      </span>
                    ) : (
                      <span className="text-muted">No match found{c.parsedTitle ? ` for "${c.parsedTitle}"` : ""}</span>
                    )}
                  </div>
                </div>
                {result && (
                  <span
                    style={{ display: "inline-flex", alignItems: "center", gap: 6 }}
                    className={result.success ? "text-muted" : "text-muted"}
                  >
                    {result.success ? <Check size={16} color="var(--status-good)" /> : <X size={16} color="var(--status-bad)" />}
                    {result.success ? "Imported" : result.error}
                  </span>
                )}
              </div>
            );
          })}

          <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
            <button
              type="button"
              className="btn btn-hero"
              onClick={commit}
              disabled={committing || selected.size === 0 || selectableCount === 0}
            >
              {committing ? "Importing…" : `Import ${selected.size} Selected`}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
