import { CaretLeftIcon as CaretLeft, CaretRightIcon as CaretRight, LinkIcon as LinkIco } from "@phosphor-icons/react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { CalendarEntry } from "../api/types";
import { useApi } from "../hooks/useApi";
import { Toggle } from "../components/Toggle";

const DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function startOfWeek(date: Date): Date {
  const d = new Date(date);
  const day = (d.getDay() + 6) % 7; // 0 = Monday
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function contentTypeHref(entry: CalendarEntry): string {
  if (entry.contentType === "movie") return `/movies/${entry.mediaItemId}`;
  if (entry.contentType === "episode") return `/shows/${entry.mediaItemId}`;
  return `/anime/${entry.mediaItemId}`;
}

export default function CalendarPage() {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [monitoredOnly, setMonitoredOnly] = useState(false);

  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => new Date(weekStart.getTime() + i * 86400000)), [weekStart]);
  const from = toIsoDate(days[0]);
  const to = toIsoDate(days[6]);

  const { data: entries, error } = useApi(() => api.calendar(from, to, monitoredOnly), [from, to, monitoredOnly]);

  const byDate = useMemo(() => {
    const map = new Map<string, CalendarEntry[]>();
    for (const entry of entries ?? []) {
      const list = map.get(entry.date) ?? [];
      list.push(entry);
      map.set(entry.date, list);
    }
    return map;
  }, [entries]);

  const todayIso = toIsoDate(new Date());
  const icsUrl = `${window.location.origin}/api/calendar.ics`;

  return (
    <div className="page with-top-padding">
      <div className="page-header">
        <div style={{ flex: 1, minWidth: 280 }}>
          <h1 style={{ marginBottom: 6 }}>Calendar</h1>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Upcoming and recent movie releases and episode air dates.
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
            <Toggle on={monitoredOnly} onChange={setMonitoredOnly} />
            Monitored only
          </label>
          <div className="seg">
            <button type="button" onClick={() => setWeekStart((w) => new Date(w.getTime() - 7 * 86400000))}>
              <CaretLeft size={14} />
            </button>
            <button type="button" onClick={() => setWeekStart(startOfWeek(new Date()))}>
              Today
            </button>
            <button type="button" onClick={() => setWeekStart((w) => new Date(w.getTime() + 7 * 86400000))}>
              <CaretRight size={14} />
            </button>
          </div>
        </div>
      </div>

      {error && <p className="text-muted">Failed to load calendar: {error}</p>}

      <div className="calendar-grid">
        {days.map((day, i) => {
          const iso = toIsoDate(day);
          const dayEntries = byDate.get(iso) ?? [];
          return (
            <div key={iso} className={`calendar-day${iso === todayIso ? " today" : ""}`}>
              <div className="calendar-day-header">
                <span>{DAY_LABELS[i]}</span>
                <span className="calendar-day-number">{day.getDate()}</span>
              </div>
              {dayEntries.map((entry) => (
                <Link
                  key={entry.mediaItemId + entry.date + (entry.episodeNumber ?? "")}
                  to={contentTypeHref(entry)}
                  className={`calendar-entry${entry.monitored ? "" : " unmonitored"}`}
                  title={entry.title}
                >
                  <span className={`dot ${entry.monitored ? "dot-good" : "dot-warn"}`} />
                  <span className="calendar-entry-title">{entry.title}</span>
                </Link>
              ))}
            </div>
          );
        })}
      </div>

      <p className="text-faint" style={{ fontSize: 12, marginTop: 20, display: "flex", alignItems: "center", gap: 6 }}>
        <LinkIco size={13} />
        Subscribe in an external calendar app: <code style={{ fontFamily: "var(--font-mono)" }}>{icsUrl}</code>
      </p>
    </div>
  );
}
