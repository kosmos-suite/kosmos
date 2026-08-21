import type { Icon } from "@phosphor-icons/react";
import {
  CompassIcon as Compass,
  DownloadSimpleIcon as DownloadSimple,
  SlidersHorizontalIcon as SlidersHorizontal,
  SquaresFourIcon as SquaresFour,
  UsersThreeIcon as UsersThree,
} from "@phosphor-icons/react";

export interface NavItem {
  to: string;
  label: string;
  icon: Icon;
}

export const navItems: NavItem[] = [
  { to: "/", label: "Discover", icon: Compass },
  { to: "/library", label: "Library", icon: SquaresFour },
  { to: "/requests", label: "Requests", icon: UsersThree },
  { to: "/activity", label: "Activity", icon: DownloadSimple },
  { to: "/settings/indexers", label: "Settings", icon: SlidersHorizontal },
];
