import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigationType } from "react-router-dom";

/**
 * Tracks an in-app navigation stack so the topbar's back/forward buttons can be genuinely disabled
 * at the ends, the way a browser's own back/forward buttons are — the History API itself exposes no
 * canGoBack/canGoForward, so this has to be reconstructed from React Router's own navigation events.
 *
 * PUSH (a real navigate/Link click) truncates anything past the current position and appends; POP
 * (back/forward, whether from the browser chrome or our own navigate(-1)/navigate(1) calls) moves
 * the index to wherever in the stack the resulting location actually matches. A POP landing on a
 * location outside the tracked stack (e.g. the user edited the URL bar, or this is the very first
 * page load) resets the stack to just that location — both back and forward correctly read as
 * unavailable from there.
 */
export function useAppHistory() {
  const location = useLocation();
  const navigationType = useNavigationType();
  const stackRef = useRef<string[]>([]);
  const indexRef = useRef(-1);
  const [, forceRender] = useState(0);

  useEffect(() => {
    const path = location.pathname + location.search;
    const stack = stackRef.current;
    const index = indexRef.current;

    if (index === -1) {
      stack.push(path);
      indexRef.current = 0;
    } else if (navigationType === "REPLACE") {
      stack[index] = path;
    } else if (navigationType === "POP" && stack[index - 1] === path) {
      indexRef.current = index - 1;
    } else if (navigationType === "POP" && stack[index + 1] === path) {
      indexRef.current = index + 1;
    } else if (navigationType === "POP") {
      stack.splice(0, stack.length, path);
      indexRef.current = 0;
    } else {
      stack.splice(index + 1);
      stack.push(path);
      indexRef.current = index + 1;
    }
    forceRender((n) => n + 1);
  }, [location, navigationType]);

  return {
    canGoBack: indexRef.current > 0,
    canGoForward: indexRef.current < stackRef.current.length - 1,
  };
}
