import { useRef, useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";

export function Layout() {
  const [scrolled, setScrolled] = useState(false);
  const scrolledRef = useRef(false);

  function handleScroll(e: React.UIEvent<HTMLElement>) {
    const next = e.currentTarget.scrollTop > 40;
    if (next !== scrolledRef.current) {
      scrolledRef.current = next;
      setScrolled(next);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-area k-scroll" onScroll={handleScroll}>
        <TopBar scrolled={scrolled} />
        <Outlet />
      </main>
    </div>
  );
}
