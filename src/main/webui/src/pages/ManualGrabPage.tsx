import {
  ArrowsLeftRightIcon as ArrowsLeftRight,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  CheckCircleIcon as CheckCircle,
  CircleNotchIcon as CircleNotch,
  DownloadSimpleIcon as DownloadSimple,
  FileArrowUpIcon as FileArrowUp,
  InfoIcon as Info,
  LinkSimpleIcon as LinkSimple,
  MagnetIcon as Magnet,
  NewspaperIcon as Newspaper,
  WarningIcon as Warning,
  XIcon as X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { useRef } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useApi } from "../hooks/useApi";
import type { DownloadClient } from "../api/types";

type Mode = "Magnet Link" | "Torrent File";
type MagnetState = "empty" | "ok" | "bad";

const CLIENT_ICON: Record<string, typeof Magnet> = {
  QBITTORRENT: Magnet,
  TRANSMISSION: ArrowsLeftRight,
  SABNZBD: Newspaper,
};

function magnetState(value: string): { state: MagnetState; prettyName: string | null; hash: string | null } {
  const m = value.trim();
  if (m.length === 0) return { state: "empty", prettyName: null, hash: null };
  const hashMatch = m.match(/xt=urn:btih:([a-zA-Z0-9]{32,40})/);
  const nameMatch = m.match(/[?&]dn=([^&]+)/);
  const ok = m.indexOf("magnet:?") === 0 && !!hashMatch;
  return {
    state: ok ? "ok" : "bad",
    prettyName: nameMatch ? decodeURIComponent(nameMatch[1].replace(/\+/g, " ")) : null,
    hash: hashMatch ? hashMatch[1] : null,
  };
}

export default function ManualGrabPage() {
  const { id } = useParams<{ id: string }>();
  const pathname = useLocation().pathname;
  const isEpisode = pathname.startsWith("/episodes/");
  const isAnimeEpisode = pathname.startsWith("/anime-episodes/");
  const navigate = useNavigate();
  const { data: movie } = useApi(
    () => (isEpisode || isAnimeEpisode ? Promise.resolve(null) : api.getMovie(id!)),
    [id, isEpisode, isAnimeEpisode],
  );
  const { data: episode } = useApi(() => (isEpisode ? api.getEpisode(id!) : Promise.resolve(null)), [id, isEpisode]);
  const { data: animeEpisode } = useApi(
    () => (isAnimeEpisode ? api.getAnimeEpisode(id!) : Promise.resolve(null)),
    [id, isAnimeEpisode],
  );
  const animeEpisodeNumber = animeEpisode ? (animeEpisode.absoluteEpisodeNumber ?? animeEpisode.episodeNumber) : null;
  const { data: clients } = useApi(() => api.listDownloadClients(), []);
  const titleLabel = movie
    ? `${movie.title}${movie.year ? ` (${movie.year})` : ""}`
    : episode
      ? `${episode.showTitle} S${String(episode.seasonNumber).padStart(2, "0")}E${String(episode.episodeNumber).padStart(2, "0")}`
      : animeEpisode
        ? `${animeEpisode.animeTitle}${animeEpisodeNumber != null ? ` - ${String(animeEpisodeNumber).padStart(2, "0")}` : ""}`
        : null;
  const resourceSegment = isAnimeEpisode ? "anime-episodes" : isEpisode ? "episodes" : "movies";

  const [mode, setMode] = useState<Mode>("Magnet Link");
  const [magnet, setMagnet] = useState("");
  const [torrentFile, setTorrentFile] = useState<File | null>(null);
  const [clientId, setClientId] = useState<string | null>(null);
  const [clientOpen, setClientOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<number | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const say = (msg: string) => {
    window.clearTimeout(toastTimer.current);
    setToast(msg);
    toastTimer.current = window.setTimeout(() => setToast(null), 3800);
  };

  const isMagnet = mode === "Magnet Link";
  const { state: mState, prettyName, hash } = magnetState(magnet);
  const curClient: DownloadClient | undefined = clients?.find((c) => c.id === clientId) ?? clients?.[0];
  const ClientIcon = (curClient && CLIENT_ICON[curClient.type]) ?? Magnet;
  const ready = !!curClient && (isMagnet ? mState === "ok" : !!torrentFile);

  const magnetHint =
    mState === "ok"
      ? prettyName
        ? `Valid · ${prettyName}`
        : `Valid · btih ${hash?.slice(0, 12)}…`
      : mState === "bad"
        ? "Needs to start with magnet:?xt=urn:btih: followed by the info hash"
        : "Starts with magnet:?xt=urn:btih: — trackers and dn are optional";

  const magnetLine = mState === "ok" ? "rgba(79,191,139,.4)" : mState === "bad" ? "rgba(224,104,95,.4)" : "var(--border)";
  const magnetFg = mState === "ok" ? "var(--status-good)" : mState === "bad" ? "var(--status-bad)" : "var(--text-ghost)";
  const MagnetHintIcon = mState === "ok" ? CheckCircle : mState === "bad" ? Warning : Info;

  const grab = async () => {
    if (!ready || sending || !curClient) return;
    setSending(true);
    try {
      const grabRelease = isEpisode || isAnimeEpisode ? api.grabEpisodeRelease : api.grabRelease;
      const grabFile = isEpisode || isAnimeEpisode ? api.grabEpisodeFile : api.grabFile;
      if (isMagnet) {
        await grabRelease(id!, {
          title: prettyName ?? titleLabel ?? magnet,
          downloadUrl: magnet,
          resolution: null,
          source: null,
          videoCodec: null,
          score: null,
          downloadClientId: curClient.id,
        });
        setMagnet("");
      } else if (torrentFile) {
        await grabFile(id!, torrentFile, titleLabel ?? torrentFile.name, curClient.id);
        setTorrentFile(null);
        if (fileInputRef.current) fileInputRef.current.value = "";
      }
      say(`Sent to ${curClient.name}`);
    } catch (e) {
      say(e instanceof ApiError ? `Grab failed: ${e.message}` : "Grab failed");
    } finally {
      setSending(false);
    }
  };

  const close = () =>
    navigate(
      movie
        ? `/movies/${id}`
        : episode
          ? `/shows/${episode.showId}`
          : animeEpisode
            ? `/anime/${animeEpisode.animeId}`
            : "/",
    );

  return (
    <div className="modal-scrim">
      <div className="modal-scrim-art" />
      <div className="modal-scrim-dim" />

      <div className="modal-scrim-center">
        <div className="grab-panel">
          <div className="grab-panel-header">
            <span className="grab-panel-icon">
              <LinkSimple size={15} />
            </span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 500, fontSize: 14, letterSpacing: "-0.01em" }}>Add manually</div>
              <div className="text-faint" style={{ fontSize: 11, marginTop: 3 }}>
                {titleLabel ?? "…"}
              </div>
            </div>
            <button type="button" className="modal-close" onClick={close}>
              <X size={14} />
            </button>
          </div>

          <div style={{ padding: "15px 18px 0" }}>
            <div className="seg" style={{ width: "100%" }}>
              {(["Magnet Link", "Torrent File"] as Mode[]).map((m) => (
                <button
                  key={m}
                  type="button"
                  className={mode === m ? "active" : ""}
                  style={{ flex: 1, display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 7 }}
                  onClick={() => setMode(m)}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>

          <div className="grab-panel-body">
            {isMagnet ? (
              <div>
                <label className="grab-field-label" htmlFor="magnet">
                  Magnet URI
                </label>
                <textarea
                  id="magnet"
                  className="grab-textarea"
                  placeholder="magnet:?xt=urn:btih:…"
                  value={magnet}
                  onChange={(e) => setMagnet(e.target.value)}
                  style={{ borderColor: magnetLine }}
                />
                <div className="grab-hint">
                  <MagnetHintIcon size={12} color={magnetFg} />
                  <span style={{ color: magnetFg, flex: 1 }}>{magnetHint}</span>
                </div>
              </div>
            ) : (
              <div>
                <label className="grab-field-label" htmlFor="torrent-file">
                  .torrent file
                </label>
                <input
                  ref={fileInputRef}
                  id="torrent-file"
                  type="file"
                  accept=".torrent,application/x-bittorrent"
                  style={{ display: "none" }}
                  onChange={(e) => setTorrentFile(e.target.files?.[0] ?? null)}
                />
                {torrentFile ? (
                  <div className="grab-picked">
                    <span className="grab-picked-icon">
                      <FileArrowUp size={14} />
                    </span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12.5, fontWeight: 500 }}>{torrentFile.name}</div>
                      <div className="text-faint" style={{ fontSize: 10.5, marginTop: 1 }}>
                        {(torrentFile.size / 1024).toFixed(0)} KB
                      </div>
                    </div>
                    <span
                      className="grab-picked-remove"
                      onClick={() => {
                        setTorrentFile(null);
                        if (fileInputRef.current) fileInputRef.current.value = "";
                      }}
                    >
                      <X size={13} />
                    </span>
                  </div>
                ) : (
                  <div className="grab-dropzone" onClick={() => fileInputRef.current?.click()}>
                    <FileArrowUp size={20} className="text-muted" />
                    <span className="lbl">Click to choose a .torrent file</span>
                    <span className="hint2">Sent straight to your download client, not scored</span>
                  </div>
                )}
              </div>
            )}

            <div style={{ marginTop: 16 }}>
              <label className="grab-field-label">Send to</label>
              {clients && clients.length === 0 ? (
                <p className="text-muted" style={{ fontSize: 12 }}>
                  No download client configured — add one under Settings.
                </p>
              ) : (
                <div className="dropdown-wrap">
                  <div className={`grab-client-trigger${clientOpen ? " open" : ""}`} onClick={() => setClientOpen((o) => !o)}>
                    <ClientIcon size={15} className="text-muted" />
                    <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>{curClient?.name ?? "…"}</span>
                    <CaretDown size={11} className="text-faint" />
                  </div>
                  {clientOpen && (
                    <div className="grab-client-menu">
                      {clients?.map((c) => {
                        const Icon = CLIENT_ICON[c.type] ?? Magnet;
                        const active = c.id === curClient?.id;
                        return (
                          <div
                            key={c.id}
                            className={`grab-client-item${active ? " active" : ""}`}
                            onClick={() => {
                              setClientId(c.id);
                              setClientOpen(false);
                            }}
                          >
                            <Icon size={14} className="text-muted" />
                            <span style={{ flex: 1, minWidth: 0, fontSize: 12.5, fontWeight: active ? 500 : 400, color: active ? "var(--text)" : "var(--text-secondary)" }}>
                              {c.name}
                            </span>
                            {active && <Check size={12} color="#B5ABFC" />}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>

            <button
              type="button"
              className="grab-submit"
              disabled={!ready || sending}
              onClick={grab}
              style={{
                background: ready ? "var(--accent-gradient)" : "rgba(233,233,237,.07)",
                border: ready ? "0" : "1px solid var(--border)",
                color: ready ? "#0B0C12" : "var(--text-muted)",
                opacity: ready ? 1 : 0.8,
                cursor: ready && !sending ? "pointer" : "not-allowed",
              }}
            >
              {sending ? <CircleNotch size={14} className="spin" /> : <DownloadSimple size={14} />}
              {sending ? "Sending…" : "Grab"}
            </button>

            <p className="grab-disclaimer">
              Manual grabs skip quality scoring — nothing is checked against your profile. Once the download
              finishes it goes through the same rename and import as anything else.
            </p>
          </div>

          <div className="grab-footer">
            <span style={{ flex: 1 }}>
              Rather pick from what the indexers have?{" "}
              <Link to={`/${resourceSegment}/${id}/search`}>Interactive Search</Link>
            </span>
          </div>
        </div>
      </div>

      {toast && (
        <div className="toast">
          <span className="toast-icon">
            <Check size={12} />
          </span>
          <span style={{ fontSize: 12.5 }}>{toast}</span>
        </div>
      )}
    </div>
  );
}
