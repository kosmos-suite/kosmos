import { CaretDownIcon as CaretDown, CheckIcon as Check } from "@phosphor-icons/react";
import { useState } from "react";

// ISO 639-1 original-language codes, matched against TMDB's own original_language field (not the
// title's spoken language) — the languages whose small, regionally-produced titles most often
// crowd out everything else in a popularity-sorted discover feed.
export const LANGUAGES: { value: string; label: string }[] = [
  { value: "zh", label: "Chinese" },
  { value: "hi", label: "Hindi" },
  { value: "ta", label: "Tamil" },
  { value: "te", label: "Telugu" },
  { value: "ml", label: "Malayalam" },
  { value: "kn", label: "Kannada" },
  { value: "bn", label: "Bengali" },
  { value: "pa", label: "Punjabi" },
  { value: "mr", label: "Marathi" },
  { value: "gu", label: "Gujarati" },
];

interface LanguageFilterDropdownProps {
  hidden: Set<string>;
  onChange: (next: Set<string>) => void;
}

/** "All Languages" / "Hide ZH,HI,…" multi-select — used by every Discover grid/list page. */
export function LanguageFilterDropdown({ hidden, onChange }: LanguageFilterDropdownProps) {
  const [open, setOpen] = useState(false);
  const codes = LANGUAGES.filter((l) => hidden.has(l.value)).map((l) => l.value.toUpperCase());

  function toggle(value: string) {
    const next = new Set(hidden);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    onChange(next);
  }

  return (
    <div className="sort-dropdown">
      <div className="seg">
        <button type="button" className={codes.length > 0 ? "active" : ""} onClick={() => setOpen((v) => !v)}>
          {codes.length === 0 ? "All Languages" : `Hide ${codes.join(",")}`}
          <CaretDown size={11} />
        </button>
      </div>
      {open && (
        <div className="sort-menu" onMouseLeave={() => setOpen(false)}>
          <button
            type="button"
            className="sort-menu-item"
            onClick={() => onChange(new Set(LANGUAGES.map((l) => l.value)))}
          >
            Hide All
          </button>
          <button type="button" className="sort-menu-item" onClick={() => onChange(new Set())}>
            Clear Selection
          </button>
          <div style={{ height: 1, background: "var(--border-subtle)", margin: "5px 4px" }} />
          {LANGUAGES.map((l) => (
            <button key={l.value} type="button" className="sort-menu-item" onClick={() => toggle(l.value)}>
              {l.label}
              {hidden.has(l.value) && <Check size={13} weight="bold" className="check" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
