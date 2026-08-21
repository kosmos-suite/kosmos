import { PlanetIcon as Planet } from "@phosphor-icons/react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const [ready, setReady] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .setupStatus()
      .then((status) => {
        if (status.needsSetup) {
          navigate("/setup", { replace: true });
        } else {
          setReady(true);
        }
      })
      .catch(() => setReady(true));
  }, [navigate]);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(username, password);
      navigate(from, { replace: true });
    } catch {
      setError("Wrong username or password.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!ready) {
    return <div className="auth-page" />;
  }

  return (
    <div className="auth-page">
      <div className="dialog">
        <div className="dialog-header">
          <span className="icon-tile">
            <Planet size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">Sign in to Kosmos</div>
            <div className="dialog-sub">Use a native Kosmos account, or your Jellyfin username and password.</div>
          </div>
        </div>

        <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div className="field">
            <label>Username</label>
            <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          </div>
          <div className="field">
            <label>Password</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <p className="text-muted">{error}</p>}
          <button type="submit" className="btn btn-hero" disabled={submitting}>
            {submitting ? "…" : "Sign in"}
          </button>
        </form>
      </div>
    </div>
  );
}
