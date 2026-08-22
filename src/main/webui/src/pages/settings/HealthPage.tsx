import {
  CheckCircleIcon as CheckCircle,
  WarningCircleIcon as WarningCircle,
  XCircleIcon as XCircle,
} from "@phosphor-icons/react";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import type { HealthCheckEntry } from "../../api/types";

const SEVERITY_META: Record<HealthCheckEntry["severity"], { kind: string; icon: JSX.Element; label: string }> = {
  OK: { kind: "good", icon: <CheckCircle size={12} weight="fill" />, label: "OK" },
  WARNING: { kind: "warn", icon: <WarningCircle size={12} weight="fill" />, label: "Warning" },
  ERROR: { kind: "bad", icon: <XCircle size={12} weight="fill" />, label: "Error" },
};

export default function HealthPage() {
  const { data: checks, error: loadError } = useApi(() => api.listSystemChecks(), []);

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Health</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Configuration and operability checks — separate from whether the Kosmos process itself is up.
          </p>
        </div>
      </div>

      {loadError && <p className="text-muted">Failed to load health checks: {loadError}</p>}

      {checks?.map((entry) => {
        const meta = SEVERITY_META[entry.severity];
        return (
          <div key={entry.source} className="indexer-row">
            <div className="indexer-row-main">
              <div className="indexer-row-title-line">
                <span className="indexer-row-name">{entry.source}</span>
                <span className={`health-pill ${meta.kind}`}>
                  {meta.icon}
                  {meta.label}
                </span>
              </div>
              {entry.message && (
                <div className="indexer-row-sub">
                  <span>{entry.message}</span>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
