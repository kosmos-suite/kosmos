import { useEffect, useRef } from "react";
import type { JobProgressEvent } from "../api/types";

/**
 * Subscribes to a job's live progress over SSE (GET /jobs/{name}/progress) for as long as this
 * component is mounted. Unlike polling, one open connection costs nothing per tick, so there's no
 * need to gate the subscription behind "is this job currently running" — staying subscribed the
 * whole time is how a client learns a run started at all, whoever triggered it, and the server
 * replays the last known event immediately on subscribe so a late mount isn't blind until the next
 * update happens to fire.
 */
export function useJobProgress(jobName: string, onEvent: (event: JobProgressEvent) => void) {
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    const source = new EventSource(`/api/jobs/${encodeURIComponent(jobName)}/progress`);
    source.onmessage = (e) => {
      try {
        onEventRef.current(JSON.parse(e.data) as JobProgressEvent);
      } catch {
        // ignore a malformed frame
      }
    };
    return () => source.close();
    // onEvent intentionally excluded — see onEventRef; including it would reconnect on every
    // render a caller passes a fresh inline closure.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobName]);
}
