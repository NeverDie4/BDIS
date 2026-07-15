import type { CurrentUser } from "@/types/api";

export type AuthStatus = "unknown" | "anonymous" | "authenticated";

export type RouteOwner =
  | "platform"
  | "auth"
  | "herb"
  | "teaching"
  | "evaluation"
  | "performance"
  | "mobile";

export type RouteMeta = {
  path: string;
  title: string;
  owner: RouteOwner;
  permission?: string;
  public?: boolean;
  navLabel?: string;
  navOrder?: number;
};

export type RouteAccess = "allowed" | "anonymous" | "forbidden" | "pending";

export function hasUserPermission(user: CurrentUser | null, permission?: string) {
  if (!permission) {
    return true;
  }
  return Boolean(user && (user.permissions.includes("*") || user.permissions.includes(permission)));
}

export function resolveAccess(
  route: RouteMeta | undefined,
  status: AuthStatus,
  user: CurrentUser | null,
): RouteAccess {
  if (!route) {
    if (status === "unknown") {
      return "pending";
    }
    return status === "anonymous" || !user ? "anonymous" : "forbidden";
  }
  if (route?.public) {
    return "allowed";
  }
  if (status === "unknown") {
    return "pending";
  }
  if (status === "anonymous" || !user) {
    return "anonymous";
  }
  return hasUserPermission(user, route?.permission) ? "allowed" : "forbidden";
}
