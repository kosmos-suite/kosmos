import { Dispatch, SetStateAction, useCallback, useEffect, useState } from "react";

interface UseApiResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
  /**
   * Patches `data` in place without a round trip — for when a mutation's own response already
   * carries the new state (e.g. one row of a list), so refetching the whole list would be wasted
   * work and would needlessly re-render every other row.
   */
  setData: Dispatch<SetStateAction<T | null>>;
}

/** Runs an async fetcher on mount and whenever `deps` changes, tracking loading/error state. */
export function useApi<T>(fetcher: () => Promise<T>, deps: unknown[] = []): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetcher()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadToken]);

  const reload = useCallback(() => setReloadToken((t) => t + 1), []);

  return { data, loading, error, reload, setData };
}
