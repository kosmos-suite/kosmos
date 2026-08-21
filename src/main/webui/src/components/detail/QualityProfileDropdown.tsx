import { CaretDownIcon as CaretDown, CheckIcon as Check, EyeIcon as Eye, EyeSlashIcon as EyeSlash } from "@phosphor-icons/react";
import { useState } from "react";
import type { QualityProfile } from "../../api/types";

/** The "Not monitored"/profile-name trigger + menu every owned detail page's monitoring control uses. */
export function QualityProfileDropdown({
  profiles,
  activeProfile,
  onSelect,
}: {
  profiles: QualityProfile[] | null;
  activeProfile: QualityProfile | null;
  onSelect: (profileId: string | null) => Promise<void>;
}) {
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  async function select(profileId: string | null) {
    setOpen(false);
    setSaving(true);
    try {
      await onSelect(profileId);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="dropdown-wrap">
      <button
        type="button"
        className={`btn btn-secondary${open ? " open" : ""}`}
        disabled={saving}
        onClick={() => setOpen((o) => !o)}
      >
        {activeProfile ? <Eye size={15} /> : <EyeSlash size={15} />}
        {saving ? "Saving…" : activeProfile ? activeProfile.name : "Not monitored"}
        <CaretDown size={11} className="text-faint" />
      </button>
      {open && (
        <div className="grab-client-menu">
          <div className={`grab-client-item${!activeProfile ? " active" : ""}`} onClick={() => select(null)}>
            <EyeSlash size={14} className="text-muted" />
            <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>Not monitored</span>
            {!activeProfile && <Check size={12} color="var(--accent-tint)" />}
          </div>
          {profiles?.map((p) => (
            <div key={p.id} className={`grab-client-item${p.id === activeProfile?.id ? " active" : ""}`} onClick={() => select(p.id)}>
              <Eye size={14} className="text-muted" />
              <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>{p.name}</span>
              {p.id === activeProfile?.id && <Check size={12} color="var(--accent-tint)" />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
