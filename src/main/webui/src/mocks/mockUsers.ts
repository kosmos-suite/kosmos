export interface UserAccount {
  id: string;
  name: string;
  handle: string;
  initials: string;
  role: "Admin" | "User";
  origin: "local" | "jellyfin";
  lastActive: string;
  /** Requests used this week, out of quotaLimit. Admins are unlimited (quotaLimit is ignored). */
  quotaUsed: number;
  quotaLimit: number;
  /** Requests per the last 5 weeks, oldest first — drives the mini bar chart next to the quota text. */
  quotaHistory: number[];
  customPermissions?: boolean;
}

/** Sample data for PermissionsPage's granular per-user permission overrides — basic auth/users are real, see UsersPage/AuthContext, but that fine-grained permission matrix isn't. */
export const mockUsers: UserAccount[] = [
  {
    id: "u1",
    name: "Alex Kruger",
    handle: "kruger",
    initials: "AK",
    role: "Admin",
    origin: "local",
    lastActive: "now",
    quotaUsed: 0,
    quotaLimit: 0,
    quotaHistory: [],
  },
  {
    id: "u2",
    name: "Priya Shah",
    handle: "priya",
    initials: "PS",
    role: "User",
    origin: "jellyfin",
    lastActive: "2 hours ago",
    quotaUsed: 5,
    quotaLimit: 5,
    quotaHistory: [2, 4, 3, 5, 5],
    customPermissions: true,
  },
  {
    id: "u3",
    name: "Sam Okafor",
    handle: "sam",
    initials: "SO",
    role: "User",
    origin: "local",
    lastActive: "yesterday",
    quotaUsed: 3,
    quotaLimit: 5,
    quotaHistory: [1, 3, 2, 4, 3],
  },
  {
    id: "u4",
    name: "Jonas Weber",
    handle: "jweber",
    initials: "JW",
    role: "User",
    origin: "jellyfin",
    lastActive: "3 days ago",
    quotaUsed: 1,
    quotaLimit: 5,
    quotaHistory: [0, 1, 0, 2, 1],
  },
  {
    id: "u5",
    name: "Mira Lindqvist",
    handle: "mira",
    initials: "ML",
    role: "User",
    origin: "local",
    lastActive: "5 days ago",
    quotaUsed: 4,
    quotaLimit: 5,
    quotaHistory: [3, 5, 4, 4, 4],
    customPermissions: true,
  },
  {
    id: "u6",
    name: "Tomas Novak",
    handle: "tnovak",
    initials: "TN",
    role: "User",
    origin: "jellyfin",
    lastActive: "2 weeks ago",
    quotaUsed: 0,
    quotaLimit: 5,
    quotaHistory: [0, 0, 1, 0, 0],
  },
];
