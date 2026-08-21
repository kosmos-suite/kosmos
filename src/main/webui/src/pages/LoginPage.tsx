import { PlanetIcon as Planet } from "@phosphor-icons/react";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const [mode, setMode] = useState<"login" | "bootstrap">("login");
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  async function handleBootstrap(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.createUser({ username, displayName, password });
      await login(username, password);
      navigate("/setup", { replace: true });
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setError("An admin account already exists — ask them to create your account instead.");
      } else if (e instanceof ApiError && e.status === 400) {
        setError("That username is already taken.");
      } else {
        setError("Could not create the account.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="dialog">
        <div className="dialog-header">
          <span className="icon-tile">
            <Planet size={16} />
          </span>
          <div className="dialog-header-body">
            <div className="dialog-title">{mode === "login" ? "Sign in to Kosmos" : "Create the admin account"}</div>
            <div className="dialog-sub">
              {mode === "login"
                ? "Use a native Kosmos account, or your Jellyfin username and password."
                : "This is the very first account — it becomes the server admin."}
            </div>
          </div>
        </div>

        <form onSubmit={mode === "login" ? handleLogin : handleBootstrap} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div className="field">
            <label>Username</label>
            <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          </div>

          {mode === "bootstrap" && (
            <div className="field">
              <label>Display name</label>
              <input className="input" value={displayName} onChange={(e) => setDisplayName(e.target.value)} required />
            </div>
          )}

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
            {submitting ? "…" : mode === "login" ? "Sign in" : "Create admin account"}
          </button>
        </form>

        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => {
            setMode((m) => (m === "login" ? "bootstrap" : "login"));
            setError(null);
          }}
        >
          {mode === "login" ? "First time setting up Kosmos? Create the admin account" : "Already have an account? Sign in"}
        </button>
      </div>
    </div>
  );
}
