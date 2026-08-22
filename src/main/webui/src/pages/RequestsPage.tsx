import {
  CheckIcon as Check,
  ChatCircleIcon as ChatCircle,
  CompassIcon as Compass,
  DotsThreeIcon as DotsThree,
  HourglassMediumIcon as HourglassMedium,
  MagnifyingGlassIcon as MagnifyingGlass,
  PaperPlaneTiltIcon as PaperPlaneTilt,
  ProhibitIcon as Prohibit,
} from "@phosphor-icons/react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { MediaRequest, RequestStatus } from "../api/types";
import { posterUrl } from "../api/tmdbImage";
import { useAuth } from "../auth/AuthContext";
import { useApi } from "../hooks/useApi";
import { declineReasonChips } from "../mocks/mockRequests";
import { tonalGradient } from "../utils/tonalGradient";

type Tab = "All" | "Pending" | "Approved" | "Available" | "Declined";

const TABS: Tab[] = ["All", "Pending", "Approved", "Available", "Declined"];
const TAB_STATUS: Record<Tab, RequestStatus | null> = {
  All: null,
  Pending: "PENDING",
  Approved: "APPROVED",
  Available: "AVAILABLE",
  Declined: "DECLINED",
};
const STATUS_META: Record<RequestStatus, { label: string; dotClass: string; tagClass: string }> = {
  PENDING: { label: "Pending", dotClass: "dot-warn", tagClass: "pending" },
  APPROVED: { label: "Approved", dotClass: "dot-good", tagClass: "approved" },
  AVAILABLE: { label: "Available", dotClass: "dot-good", tagClass: "available" },
  DECLINED: { label: "Declined", dotClass: "dot-bad", tagClass: "declined" },
};
const KIND_LABEL: Record<MediaRequest["mediaType"], string> = { movie: "Movie", tv: "Series" };

function relativeTime(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(ms / 60_000);
  if (mins < 60) return mins <= 1 ? "just now" : `${mins} minutes ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return hours === 1 ? "1 hour ago" : `${hours} hours ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "yesterday";
  if (days < 7) return `${days} days ago`;
  return "last week";
}

interface Toast {
  message: string;
  kind: "ok" | "no";
}

export default function RequestsPage() {
  const { user } = useAuth();
  const admin = user?.role === "ADMIN";
  const { data, reload } = useApi(() => api.listRequests(), []);
  const [tab, setTab] = useState<Tab>("All");
  const [decliningId, setDecliningId] = useState<string | null>(null);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [neverSuggest, setNeverSuggest] = useState<Record<string, boolean>>({});
  const [toast, setToast] = useState<Toast | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const all = data ?? [];
  const mine = useMemo(() => all.filter((r) => r.mine), [all]);
  const scope = admin ? all : mine;

  const counts: Record<Tab, number> = {
    All: scope.length,
    Pending: scope.filter((r) => r.status === "PENDING").length,
    Approved: scope.filter((r) => r.status === "APPROVED").length,
    Available: scope.filter((r) => r.status === "AVAILABLE").length,
    Declined: scope.filter((r) => r.status === "DECLINED").length,
  };
  const rows = scope.filter((r) => !TAB_STATUS[tab] || r.status === TAB_STATUS[tab]);
  const pendingCount = counts.Pending;

  function say(message: string, kind: Toast["kind"]) {
    setToast({ message, kind });
    setTimeout(() => setToast(null), 3400);
  }

  async function approve(request: MediaRequest) {
    setBusyId(request.id);
    try {
      await api.approveRequest(request.id);
      await reload();
      say(`${request.title} approved — searching indexers`, "ok");
    } catch (err) {
      say(err instanceof Error ? err.message : "Failed to approve", "no");
    } finally {
      setBusyId(null);
    }
  }

  async function confirmDecline(request: MediaRequest) {
    const why = (reasons[request.id] ?? "").trim();
    setBusyId(request.id);
    try {
      await api.declineRequest(request.id, why || null);
      if (neverSuggest[request.id]) {
        await api.excludeFromImportLists({
          pluginSlug: request.pluginSlug,
          externalId: request.externalId,
          title: request.title,
        });
      }
      await reload();
      setDecliningId(null);
      say(`${request.title} declined`, "no");
    } catch (err) {
      say(err instanceof Error ? err.message : "Failed to decline", "no");
    } finally {
      setBusyId(null);
    }
  }

  const emptyCopy = admin
    ? { title: "Nothing in this view", body: "No requests match the filter. Try All, or check back once someone asks for something." }
    : tab === "All"
      ? { title: "You haven't requested anything yet", body: "Find something you want on Discover and hit Request — you'll see it here while an admin looks at it." }
      : { title: `Nothing ${tab.toLowerCase()} right now`, body: "Your other requests are under the remaining tabs. Discover is where new ones start." };

  return (
    <div className="page with-top-padding">
      <div className="page-header" style={{ alignItems: "flex-start", justifyContent: "space-between" }}>
        <div>
          <h1>Requests</h1>
          <p className="text-muted" style={{ margin: 0 }}>
            {admin
              ? "Everything anyone has asked for — approve or decline without leaving the list."
              : "What you've asked for, and where each one stands."}
          </p>
        </div>
      </div>

      <div className="filter-tabs" style={{ alignItems: "center" }}>
        {TABS.map((t) => {
          const status = TAB_STATUS[t];
          return (
            <button key={t} className={tab === t ? "active" : ""} onClick={() => setTab(t)}>
              {status && <span className={`dot ${STATUS_META[status].dotClass}`} style={{ marginRight: 6 }} />}
              {t} <span className="text-faint">{counts[t]}</span>
            </button>
          );
        })}
        <div style={{ flex: 1 }} />
        <span className="text-muted" style={{ fontSize: 12.5, display: "inline-flex", alignItems: "center", gap: 6 }}>
          <MagnifyingGlass size={13} />
          Newest first
        </span>
      </div>

      {admin && pendingCount > 0 && (
        <div className="pending-banner">
          <span className="pending-banner-icon">
            <HourglassMedium size={15} />
          </span>
          <div style={{ flex: 1 }}>
            <div className="pending-banner-title">
              {pendingCount} {pendingCount === 1 ? "request needs" : "requests need"} your review
            </div>
            <div className="pending-banner-sub">Approving starts a search straight away.</div>
          </div>
          <button type="button" className="btn btn-secondary" onClick={() => setTab("Pending")}>
            Review pending
          </button>
        </div>
      )}

      {rows.map((request, i) => {
        const meta = STATUS_META[request.status];
        const declining = decliningId === request.id;
        const canAct = admin && request.status === "PENDING" && !declining;
        const poster = posterUrl(request.posterPath);

        return (
          <div key={request.id} className="request-card">
            <div className="request-card-main">
              <div
                className="poster-thumb"
                style={poster ? { backgroundImage: `url(${poster})`, backgroundSize: "cover" } : { background: tonalGradient(i) }}
              />
              <div className="request-card-body">
                <div className="request-card-title-row">
                  <span style={{ fontWeight: 500 }}>{request.title}</span>
                  {request.year && <span className="text-faint">{request.year}</span>}
                  <span className="kind-tag">{KIND_LABEL[request.mediaType]}</span>
                </div>
                <div className="request-card-meta">
                  {admin && (
                    <span className="requester">
                      <span className="requester-avatar">{request.requestedByDisplayName.slice(0, 2).toUpperCase()}</span>
                      {request.requestedByDisplayName}
                    </span>
                  )}
                  <span className="text-faint">{relativeTime(request.requestedAt)}</span>
                  <span className="text-disabled" style={{ fontFamily: "var(--font-mono)" }}>
                    {request.qualityProfileName ?? "no profile picked"}
                  </span>
                </div>
              </div>

              <span className={`status-tag ${meta.tagClass}`}>
                <span className={`dot ${meta.dotClass}`} />
                {meta.label}
              </span>

              {canAct ? (
                <div style={{ display: "flex", gap: 7 }}>
                  <button
                    type="button"
                    className="btn btn-hero"
                    disabled={busyId === request.id}
                    onClick={() => approve(request)}
                  >
                    <Check size={13} weight="bold" />
                    Approve
                  </button>
                  <button type="button" className="btn btn-secondary" onClick={() => setDecliningId(request.id)}>
                    Decline
                  </button>
                </div>
              ) : (
                !declining && (
                  <button type="button" className="btn btn-icon" aria-label="More actions">
                    <DotsThree size={16} />
                  </button>
                )
              )}
            </div>

            {declining && (
              <div className="decline-panel">
                <span style={{ fontSize: 11.5, fontWeight: 500, color: "var(--status-bad-text)", flex: "none" }}>Reason</span>
                <span className="text-faint" style={{ fontSize: 11, flex: "none" }}>
                  optional
                </span>
                {declineReasonChips.map((chip) => (
                  <button
                    key={chip}
                    type="button"
                    className={`reason-chip${reasons[request.id] === chip ? " active" : ""}`}
                    onClick={() => setReasons((s) => ({ ...s, [request.id]: chip }))}
                  >
                    {chip}
                  </button>
                ))}
                <input
                  value={reasons[request.id] ?? ""}
                  onChange={(e) => setReasons((s) => ({ ...s, [request.id]: e.target.value }))}
                  placeholder="or type a note the requester will see"
                />
                <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 11.5, flex: "none" }}>
                  <input
                    type="checkbox"
                    checked={neverSuggest[request.id] ?? false}
                    onChange={(e) => setNeverSuggest((s) => ({ ...s, [request.id]: e.target.checked }))}
                  />
                  Never suggest again
                </label>
                <button type="button" className="btn btn-ghost" onClick={() => setDecliningId(null)}>
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn"
                  disabled={busyId === request.id}
                  style={{ background: "rgba(224,104,95,.18)", border: "1px solid rgba(224,104,95,.34)", color: "var(--status-bad-text)" }}
                  onClick={() => confirmDecline(request)}
                >
                  Decline request
                </button>
              </div>
            )}

            {request.note && !declining && (
              <div className="request-note">
                <ChatCircle size={13} />
                <span>{request.note}</span>
              </div>
            )}
          </div>
        );
      })}

      {rows.length === 0 && (
        <div className="empty-state">
          <div className="empty-state-inner">
            <div
              style={{
                position: "relative",
                width: 150,
                height: 112,
                margin: "0 auto 30px",
              }}
            >
              <div
                style={{
                  position: "absolute",
                  left: 6,
                  top: 12,
                  width: 58,
                  aspectRatio: "2 / 3",
                  borderRadius: 10,
                  background: "#14151f",
                  border: "1px dashed var(--border-strong)",
                  transform: "rotate(-8deg)",
                }}
              />
              <div
                style={{
                  position: "absolute",
                  right: 6,
                  top: 12,
                  width: 58,
                  aspectRatio: "2 / 3",
                  borderRadius: 10,
                  background: "#14151f",
                  border: "1px dashed var(--border-strong)",
                  transform: "rotate(8deg)",
                }}
              />
              <div
                style={{
                  position: "absolute",
                  left: "50%",
                  top: 0,
                  transform: "translateX(-50%)",
                  width: 62,
                  aspectRatio: "2 / 3",
                  borderRadius: 11,
                  background: "var(--bg)",
                  border: "1px solid rgba(145,132,217,.28)",
                  display: "grid",
                  placeItems: "center",
                  boxShadow: "0 16px 36px rgba(0,0,0,.5)",
                }}
              >
                <PaperPlaneTilt size={22} color="var(--accent-tint)" />
              </div>
            </div>
            <div className="empty-state-title">{emptyCopy.title}</div>
            <p className="empty-state-body">{emptyCopy.body}</p>
            <div className="empty-state-actions">
              <Link to="/" className="btn btn-hero">
                <Compass size={15} />
                Browse Discover
              </Link>
              <Link to="/search" className="btn btn-secondary">
                <MagnifyingGlass size={15} />
                Search for a title
              </Link>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="toast">
          <span
            className="toast-icon"
            style={{
              background: toast.kind === "ok" ? "rgba(79,191,139,.18)" : "rgba(224,104,95,.18)",
              color: toast.kind === "ok" ? "var(--status-good-text)" : "var(--status-bad-text)",
            }}
          >
            {toast.kind === "ok" ? <Check size={12} /> : <Prohibit size={12} />}
          </span>
          <span style={{ fontSize: 12.5 }}>{toast.message}</span>
        </div>
      )}
    </div>
  );
}
