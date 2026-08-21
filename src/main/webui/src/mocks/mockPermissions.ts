export type PermissionKey =
  | "reqMovies"
  | "reqTv"
  | "req4k"
  | "autoApprove"
  | "viewActivity"
  | "manageQuality"
  | "manageIndexers"
  | "managePlugins"
  | "manageUsers";

export interface PermissionDef {
  key: PermissionKey;
  label: string;
  help: string;
}

export interface PermissionSection {
  id: string;
  title: string;
  note: string;
  permissions: PermissionDef[];
}

/** No permissions system exists yet — Kosmos has no roles/auth at all today. */
export const permissionSections: PermissionSection[] = [
  {
    id: "requests",
    title: "Requests",
    note: "What people can ask the library for",
    permissions: [
      { key: "reqMovies", label: "Request movies", help: "Submit movie requests for admin approval" },
      { key: "reqTv", label: "Request TV", help: "Submit TV show requests for admin approval" },
      { key: "req4k", label: "Request 4K content", help: "Request the 4K quality profile specifically" },
      { key: "autoApprove", label: "Auto-approve requests", help: "Requests are approved immediately, no admin review" },
    ],
  },
  {
    id: "library",
    title: "Library",
    note: "Visibility into what's happening",
    permissions: [
      { key: "viewActivity", label: "View activity & downloads", help: "See what's currently downloading and its progress" },
      { key: "manageQuality", label: "Manage quality profiles", help: "Edit scoring rules, formats, and cutoffs" },
    ],
  },
  {
    id: "server",
    title: "Server",
    note: "Configuration that affects everyone",
    permissions: [
      { key: "manageIndexers", label: "Manage indexers", help: "Add, remove, or edit indexer connections" },
      { key: "managePlugins", label: "Manage plugins", help: "Enable, disable, or configure metadata plugins" },
      { key: "manageUsers", label: "Manage users", help: "Invite, remove, or change other users' roles" },
    ],
  },
];

export const userRoleDefaults: Record<PermissionKey, boolean> = {
  reqMovies: true,
  reqTv: true,
  req4k: false,
  autoApprove: false,
  viewActivity: true,
  manageQuality: false,
  manageIndexers: false,
  managePlugins: false,
  manageUsers: false,
};

export type OverrideValue = "default" | "allow" | "deny";

/** Keyed by UserAccount.id from mockUsers.ts — only non-default keys need an entry. */
export const permissionOverrides: Record<string, Partial<Record<PermissionKey, OverrideValue>>> = {
  u2: { autoApprove: "allow", req4k: "allow" },
  u3: {},
  u4: { reqTv: "deny" },
  u5: { manageQuality: "allow", viewActivity: "allow" },
  u6: {},
};
