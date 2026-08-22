import { useEffect } from "react";
import { api } from "../api/client";
import type { ScheduledJob } from "../api/types";

const JOB_POLL_MS = 1000;

/**
 * Polls one job's live state (GET /jobs/{name}) while `active`, calling `onUpdate` with each
 * fresh read. A run can be started by a different tab, a different user, or (once scheduling is
 * turned on) the recurring tick — polling ambient server state, rather than only tracking a click
 * made in this component, is what lets the UI reflect that regardless of who started it.
 */
export function useJobPoll(jobName: string, active: boolean, onUpdate: (job: ScheduledJob) => void) {
  useEffect(() => {
    if (!active) return;
    let cancelled = false;
    const poll = () => {
      api
        .getJob(jobName)
        .then((job) => {
          if (!cancelled) onUpdate(job);
        })
        .catch(() => undefined);
    };
    poll();
    const id = window.setInterval(poll, JOB_POLL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobName, active]);
}
