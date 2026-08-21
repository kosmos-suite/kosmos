import {
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  LockIcon as Lock,
  MagnifyingGlassIcon as MagnifyingGlass,
  ShieldCheckIcon as ShieldCheck,
  UserIcon,
} from "@phosphor-icons/react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Toggle } from "../../components/Toggle";
import {
  permissionOverrides,
  permissionSections,
  userRoleDefaults,
  type OverrideValue,
  type PermissionKey,
} from "../../mocks/mockPermissions";
import { mockUsers } from "../../mocks/mockUsers";

/** No granular per-user permission system exists yet — basic auth/roles are real, see UsersPage/AuthContext. */

type Role = "Admin" | "User";
const ALL_KEYS = permissionSections.flatMap((s) => s.permissions.map((p) => p.key));

export default function PermissionsPage() {
  const [role, setRole] = useState<Role>("User");
  const [defaults, setDefaults] = useState<Record<PermissionKey, boolean>>(userRoleDefaults);
  const [overrides, setOverrides] = useState<Record<string, Partial<Record<PermissionKey, OverrideValue>>>>(permissionOverrides);
  const [search, setSearch] = useState("");
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function say(message: string) {
    setToast(message);
    setTimeout(() => setToast(null), 3400);
  }

  const overrideCounts = useMemo(() => {
    const counts: Partial<Record<PermissionKey, number>> = {};
    for (const key of ALL_KEYS) {
      counts[key] = Object.values(overrides).filter((o) => o[key] === "allow" || o[key] === "deny").length;
    }
    return counts;
  }, [overrides]);

  const filteredUsers = mockUsers.filter(
    (u) => u.name.toLowerCase().includes(search.toLowerCase()) || u.handle.toLowerCase().includes(search.toLowerCase()),
  );

  /** Count of accounts that have at least one non-default permission, independent of the currently selected role tab. */
  const usersWithOverrides = mockUsers.filter((u) =>
    Object.values(overrides[u.id] ?? {}).some((v) => v && v !== "default"),
  ).length;

  function setOverride(userId: string, key: PermissionKey, value: OverrideValue) {
    setOverrides((s) => {
      const current = { ...(s[userId] ?? {}) };
      if (value === "default") delete current[key];
      else current[key] = value;
      return { ...s, [userId]: current };
    });
  }

  function resetUser(userId: string, name: string) {
    setOverrides((s) => ({ ...s, [userId]: {} }));
    say(`${name}'s permissions reset to role defaults`);
  }

  const adminCount = ALL_KEYS.length;
  const userCount = ALL_KEYS.filter((k) => defaults[k]).length;

  return (
    <div>
      <div className="page-header" style={{ padding: "0 0 6px" }}>
        <h1>Permissions</h1>
      </div>
      <p className="text-muted" style={{ marginBottom: 20 }}>
        Set what each role can do by default, then fine-tune individual people under{" "}
        <Link to="/settings/users">Users & access</Link>.
      </p>

      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 10 }}>
        <div className="role-seg">
          <button className={role === "Admin" ? "active" : ""} onClick={() => setRole("Admin")}>
            <ShieldCheck size={14} />
            Admin
            <span className="role-seg-count">{adminCount}/9</span>
          </button>
          <button className={role === "User" ? "active" : ""} onClick={() => setRole("User")}>
            <UserIcon size={14} />
            User
            <span className="role-seg-count">{userCount}/9</span>
          </button>
        </div>
        <span className="text-faint" style={{ fontSize: 12 }}>
          {role === "Admin" ? adminCount : userCount} of {ALL_KEYS.length} permissions granted · {usersWithOverrides} per-user override{usersWithOverrides === 1 ? "" : "s"}
        </span>
      </div>
      <p className="text-faint" style={{ fontSize: 12, marginTop: 10 }}>
        {role === "Admin" ? "Admin is the highest role — every permission is always on." : "These are the defaults new users get. Override any of them for one person below."}
      </p>

      {role === "Admin" ? (
        <div className="lock-banner">
          <Lock size={15} />
          Admin permissions can't be edited — the role always has full access to keep at least one account able to
          fix anything.
        </div>
      ) : (
        permissionSections.map((section) => (
          <div key={section.id} className="permission-group">
            <div style={{ display: "flex", alignItems: "baseline", gap: 8, marginTop: 12, marginBottom: 2 }}>
              <h3 style={{ margin: 0 }}>{section.title}</h3>
              <span className="text-faint" style={{ fontSize: 12 }}>
                {section.note}
              </span>
            </div>
            {section.permissions.map((perm) => (
              <div key={perm.key} className="permission-row">
                <div className="permission-row-main">
                  <div className="permission-row-label" style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    {perm.label}
                    {!!overrideCounts[perm.key] && (
                      <span className="permission-row-badge">
                        {overrideCounts[perm.key]} override{overrideCounts[perm.key] === 1 ? "" : "s"}
                      </span>
                    )}
                  </div>
                  <div className="permission-row-desc">{perm.help}</div>
                </div>
                <Toggle on={defaults[perm.key]} onChange={(next) => setDefaults((s) => ({ ...s, [perm.key]: next }))} label={perm.label} />
              </div>
            ))}
          </div>
        ))
      )}

      <h3 style={{ marginTop: 32, marginBottom: 4 }}>Per-user overrides</h3>
      <p className="text-muted" style={{ marginBottom: 4, fontSize: 13 }}>
        Grant or deny a specific permission for one person without changing their whole role.
      </p>
      <p className="text-muted" style={{ marginBottom: 14, fontSize: 13 }}>
        Anyone with an override carries a{" "}
        <span className="tag tag-outline" style={{ fontSize: 10 }}>
          Custom
        </span>{" "}
        tag on Users & access.
      </p>

      <div className="overrides-search">
        <MagnifyingGlass size={14} />
        <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Find a person…" />
      </div>

      {filteredUsers.length === 0 && <p className="text-faint">No one matches "{search}".</p>}

      {filteredUsers.map((user) => {
        const isAdmin = user.role === "Admin";
        const userOverrides = overrides[user.id] ?? {};
        const overrideCount = Object.values(userOverrides).filter((v) => v && v !== "default").length;
        const expanded = expandedId === user.id;
        const summary = isAdmin
          ? "Admin — every permission, not overridable"
          : overrideCount === 0
            ? `Follows the ${user.role} role exactly`
            : `${overrideCount} permission${overrideCount === 1 ? "" : "s"} differ from the ${user.role} role`;

        return (
          <div key={user.id} className="override-row">
            <div
              className="override-row-head"
              onClick={() => !isAdmin && setExpandedId(expanded ? null : user.id)}
              style={isAdmin ? { cursor: "default" } : undefined}
            >
              <span className="avatar">{user.initials}</span>
              <div className="override-row-main">
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ fontWeight: 500, fontSize: 13.5 }}>{user.name}</span>
                  <span className={`role-badge ${user.role}`}>{user.role}</span>
                  {!isAdmin && overrideCount > 0 && (
                    <span className="tag tag-outline" style={{ fontSize: 10 }}>
                      Custom
                    </span>
                  )}
                </div>
                <div className="override-row-summary">{summary}</div>
              </div>
              {!isAdmin && overrideCount > 0 && (
                <span
                  className="btn btn-ghost"
                  style={{ fontSize: 11.5 }}
                  onClick={(e) => {
                    e.stopPropagation();
                    resetUser(user.id, user.name);
                  }}
                >
                  Reset to role
                </span>
              )}
              {!isAdmin && <CaretDown size={14} className={`override-row-chevron${expanded ? " open" : ""}`} />}
            </div>

            {!isAdmin && expanded && (
              <div className="override-panel">
                {permissionSections.map((section) =>
                  section.permissions.map((perm) => {
                    const value: OverrideValue = userOverrides[perm.key] ?? "default";
                    return (
                      <div key={perm.key} className="override-perm-row">
                        <div className="override-perm-main">
                          <div style={{ fontSize: 13 }}>{perm.label}</div>
                          <div className="override-perm-sub">
                            {section.title} · role default {defaults[perm.key] ? "on" : "off"}
                          </div>
                        </div>
                        <div className="seg-3way">
                          {(["default", "allow", "deny"] as const).map((v) => (
                            <button key={v} className={value === v ? `active-${v}` : ""} onClick={() => setOverride(user.id, perm.key, v)}>
                              {v === "default" ? "Default" : v === "allow" ? "Allow" : "Deny"}
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  }),
                )}
              </div>
            )}
          </div>
        );
      })}

      {toast && (
        <div className="toast">
          <span className="toast-icon" style={{ background: "rgba(79,191,139,.18)", color: "#7fd6ac" }}>
            <Check size={12} />
          </span>
          <span style={{ fontSize: 12.5 }}>{toast}</span>
        </div>
      )}
    </div>
  );
}
