import { ListBulletsIcon as ListBullets, PlusIcon as Plus, TrashIcon as Trash, XIcon as X } from "@phosphor-icons/react";
import { useState } from "react";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import type { ImportList, ImportListSourceType, QualityProfile } from "../../api/types";
import { Toggle } from "../../components/Toggle";
import { QualityProfileDropdown } from "../../components/detail/QualityProfileDropdown";

const SOURCE_TYPES: { value: ImportListSourceType; label: string }[] = [
  { value: "TMDB_POPULAR_MOVIES", label: "TMDB Popular Movies" },
  { value: "TMDB_UPCOMING_MOVIES", label: "TMDB Upcoming Movies" },
  { value: "TMDB_TRENDING_MOVIES", label: "TMDB Trending Movies" },
  { value: "TMDB_POPULAR_TV", label: "TMDB Popular Series" },
  { value: "TMDB_UPCOMING_TV", label: "TMDB Upcoming Series" },
  { value: "TMDB_TRENDING_TV", label: "TMDB Trending Series" },
];
const SOURCE_LABEL: Record<ImportListSourceType, string> = Object.fromEntries(
  SOURCE_TYPES.map((t) => [t.value, t.label]),
) as Record<ImportListSourceType, string>;

export default function ImportListsPage() {
  const { data: lists, error: loadError, reload } = useApi(() => api.listImportLists(), []);
  const { data: exclusions, reload: reloadExclusions } = useApi(() => api.listImportListExclusions(), []);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const [modalOpen, setModalOpen] = useState(false);

  async function toggle(list: ImportList, field: "enabled" | "trusted") {
    await api.updateImportList(list.id, {
      name: list.name,
      enabled: field === "enabled" ? !list.enabled : list.enabled,
      trusted: field === "trusted" ? !list.trusted : list.trusted,
      qualityProfileId: list.qualityProfileId,
    });
    reload();
  }

  async function setProfile(list: ImportList, profileId: string | null) {
    await api.updateImportList(list.id, {
      name: list.name,
      enabled: list.enabled,
      trusted: list.trusted,
      qualityProfileId: profileId,
    });
    reload();
  }

  async function remove(id: string) {
    await api.deleteImportList(id);
    reload();
  }

  async function removeExclusion(id: string) {
    await api.removeImportListExclusion(id);
    reloadExclusions();
  }

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 4px" }}>
        <div style={{ flex: 1, minWidth: 280 }}>
          <h2 style={{ marginBottom: 6 }}>Import Lists</h2>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Feeds a new title into Requests for approval when it appears — same queue a user request goes
            through. Turn on "Trusted" for a list to skip approval and add automatically.
          </p>
        </div>
        <button type="button" className="btn btn-hero" onClick={() => setModalOpen(true)}>
          <Plus size={15} weight="bold" />
          Add Import List
        </button>
      </div>

      {loadError && <p className="text-muted">Failed to load import lists: {loadError}</p>}
      {lists?.length === 0 && <p className="text-muted">No import lists configured yet.</p>}

      {lists?.map((list) => {
        const activeProfile = profiles?.find((p) => p.id === list.qualityProfileId) ?? null;
        return (
          <div key={list.id} className={`indexer-row${list.enabled ? "" : " disabled"}`}>
            <span className="icon-tile">
              <ListBullets size={16} />
            </span>
            <div className="indexer-row-main">
              <div className="indexer-row-title-line">
                <span className="indexer-row-name">{list.name}</span>
                <span className="protocol-pill">{SOURCE_LABEL[list.sourceType]}</span>
              </div>
              <div className="indexer-row-sub">
                <span>{list.lastSyncedAt ? `last synced ${new Date(list.lastSyncedAt).toLocaleString()}` : "never synced"}</span>
              </div>
            </div>
            <QualityProfileDropdown
              profiles={profiles as QualityProfile[] | null}
              activeProfile={activeProfile}
              onSelect={(id) => setProfile(list, id)}
            />
            <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 12 }}>
              <Toggle on={list.trusted} onChange={() => toggle(list, "trusted")} />
              Trusted
            </label>
            <Toggle on={list.enabled} onChange={() => toggle(list, "enabled")} />
            <button type="button" className="btn-icon icon-btn-danger" onClick={() => remove(list.id)} title="Remove">
              <Trash size={15} />
            </button>
          </div>
        );
      })}

      <h2 style={{ margin: "32px 0 6px" }}>Exclusions</h2>
      <p className="text-muted" style={{ maxWidth: "60ch", marginBottom: 12 }}>
        Titles no list will ever suggest again — check "Never suggest again" when declining a request to add
        one, or manage the list here.
      </p>
      {exclusions?.length === 0 && <p className="text-muted">No exclusions yet.</p>}
      {exclusions?.map((exclusion) => (
        <div key={exclusion.id} className="indexer-row">
          <div className="indexer-row-main">
            <span className="indexer-row-name">{exclusion.title}</span>
          </div>
          <button
            type="button"
            className="btn-icon icon-btn-danger"
            onClick={() => removeExclusion(exclusion.id)}
            title="Remove exclusion"
          >
            <Trash size={15} />
          </button>
        </div>
      ))}

      {modalOpen && (
        <AddImportListModal
          onClose={() => setModalOpen(false)}
          onCreated={() => {
            setModalOpen(false);
            reload();
          }}
        />
      )}
    </div>
  );
}

function AddImportListModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [sourceType, setSourceType] = useState<ImportListSourceType>("TMDB_POPULAR_MOVIES");
  const [trusted, setTrusted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid = name.trim().length > 0;

  async function save() {
    if (!valid || saving) return;
    setSaving(true);
    setError(null);
    try {
      await api.createImportList({ name, sourceType, trusted, qualityProfileId: null });
      onCreated();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="dialog-backdrop" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <span className="icon-tile">
            <ListBullets size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Add import list</div>
            <div className="dialog-sub">New matches go to Requests for approval, unless Trusted.</div>
          </div>
          <button type="button" className="dialog-close" onClick={onClose}>
            <X size={15} />
          </button>
        </div>

        <div className="field">
          <label>Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="TMDB Popular" />
        </div>

        <div className="field">
          <label>Source</label>
          <div className="seg" style={{ flexWrap: "wrap", height: "auto" }}>
            {SOURCE_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                className={sourceType === t.value ? "active" : ""}
                onClick={() => setSourceType(t.value)}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
          <Toggle on={trusted} onChange={setTrusted} />
          Trusted — auto-add without approval
        </label>

        {error && <p className="text-muted">{error}</p>}

        <div className="dialog-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="button" className="btn btn-hero" onClick={save} disabled={!valid || saving}>
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}
