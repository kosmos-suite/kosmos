const HIDE_DELAY_MS = 650;

const hideTimers = new WeakMap<Element, ReturnType<typeof setTimeout>>();

/**
 * Toggles `.is-scrolling` on any `.k-scroll` element while it's actively being scrolled, then
 * removes it after a short idle delay — one capture-phase listener covers every current and future
 * `.k-scroll` element (scroll events don't bubble, so this has to run in the capture phase).
 */
export function initScrollbarVisibility() {
  document.addEventListener(
    "scroll",
    (event) => {
      const target = event.target;
      if (!(target instanceof Element)) return;
      const scroller = target.closest(".k-scroll");
      if (!scroller) return;

      scroller.classList.add("is-scrolling");
      const existing = hideTimers.get(scroller);
      if (existing) clearTimeout(existing);
      hideTimers.set(
        scroller,
        setTimeout(() => scroller.classList.remove("is-scrolling"), HIDE_DELAY_MS),
      );
    },
    { capture: true, passive: true },
  );
}
