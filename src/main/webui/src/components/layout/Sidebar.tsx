import { CaretDoubleLeftIcon as CaretDoubleLeft, CaretDoubleRightIcon as CaretDoubleRight } from "@phosphor-icons/react";
import { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { api } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useApi } from "../../hooks/useApi";
import { activeDownloadSources } from "../../mocks/mockActivity";
import { formatTb } from "../../utils/formatBytes";
import { navItems } from "./navItems";

const activeJobCount = activeDownloadSources.filter((d) => d.state === "downloading").length;

/** Collections sidebar tints — no smart-collections feature exists yet (see LibraryResource.stats), just these three real content-type counts. */
const COLLECTION_TINTS: Record<string, string> = {
  Movies: "#9184D9",
  Series: "#5AC8DC",
  Anime: "#7FD6AC",
};

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const showCollections = location.pathname.startsWith("/library");
  const showDiskFree = location.pathname.startsWith("/activity");
  const { user } = useAuth();
  const { data: requests } = useApi(() => api.listRequests(), [user?.id]);
  const { data: stats } = useApi(() => api.libraryStats(), []);
  const pendingRequestCount = requests?.filter((r) => r.status === "PENDING").length ?? 0;
  const libraryCount = stats ? stats.movieCount + stats.seriesCount + stats.animeCount : null;

  const navCounts: Record<string, string> = {
    "/library": libraryCount != null ? libraryCount.toLocaleString() : "",
    "/requests": String(pendingRequestCount),
    "/activity": String(activeJobCount),
  };

  const collections = stats
    ? [
        { label: "Movies", count: stats.movieCount },
        { label: "Series", count: stats.seriesCount },
        { label: "Anime", count: stats.animeCount },
      ]
    : [];
  const storageUsedPct =
    stats?.totalBytes && stats.totalBytes > 0 ? Math.round((stats.usedBytes / stats.totalBytes) * 100) : 0;
  const storageUsedLabel = stats
    ? stats.totalBytes != null
      ? `${formatTb(stats.usedBytes)} of ${formatTb(stats.totalBytes)} used`
      : `${formatTb(stats.usedBytes)} used`
    : "";
  const storageFreeLabel =
    stats?.totalBytes != null ? `${formatTb(stats.totalBytes - stats.usedBytes)} free` : "";

  return (
    <aside className={`sidebar${collapsed ? " collapsed" : ""}`}>
      <div className="sidebar-brand">
        <span className="sidebar-mark" aria-hidden />
        <span className="sidebar-wordmark">Kosmos</span>
      </div>

      <nav>
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) => `nav-item${isActive ? " active" : ""}`}
          >
            <span className="nav-item-icon">
              <Icon size={18} weight="regular" />
            </span>
            <span className="nav-item-label">{label}</span>
            {navCounts[to] && navCounts[to] !== "0" && <span className="nav-item-count">{navCounts[to]}</span>}
          </NavLink>
        ))}
      </nav>

      {showCollections && (
        <>
          <div className="sidebar-section-label">Collections</div>
          {collections.map((c) => (
            <div key={c.label} className="sidebar-collection">
              <span className="sidebar-collection-dot" style={{ background: COLLECTION_TINTS[c.label] }} />
              <span className="sidebar-collection-label">{c.label}</span>
              <span className="sidebar-collection-count">{c.count}</span>
            </div>
          ))}
        </>
      )}

      <div className="sidebar-spacer" />

      {showCollections && (
        <div className="sidebar-storage">
          <span className="dot dot-good" />
          <span className="sidebar-storage-text">{storageUsedLabel}</span>
        </div>
      )}

      {showDiskFree && (
        <div className="sidebar-disk">
          <div className="sidebar-disk-row">
            <span className="sidebar-disk-label">Disk</span>
            <span className="sidebar-disk-value">{storageFreeLabel}</span>
          </div>
          <div className="progress-track">
            <div className="progress-fill sidebar-disk-fill" style={{ width: `${storageUsedPct}%` }} />
          </div>
        </div>
      )}

      <button type="button" className="sidebar-toggle" onClick={() => setCollapsed((c) => !c)}>
        {collapsed ? <CaretDoubleRight size={16} /> : <CaretDoubleLeft size={16} />}
        <span className="sidebar-toggle-label">Collapse</span>
      </button>

      <div className="sidebar-status">
        <span className="dot dot-good" />
        <span className="sidebar-status-text">
          Server healthy{activeJobCount > 0 ? ` · ${activeJobCount} jobs` : ""}
        </span>
      </div>
    </aside>
  );
}
