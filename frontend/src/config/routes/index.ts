import type { CurrentUser } from "@/types/api";
import { resolveSafeReturnUrl } from "@/lib/auth-navigation";
import { authAdminRoutes } from "./auth-admin";
import { businessRoutes } from "./business";
import { publicRoutes } from "./public";
import type { AuthStatus, RouteMeta } from "./types";
import { hasUserPermission } from "./types";

export type { AuthStatus, RouteAccess, RouteMeta } from "./types";
export { resolveAccess } from "./types";

export const appRoutes = [...publicRoutes, ...businessRoutes, ...authAdminRoutes];

function normalizePath(path: string) {
  if (path === "/") {
    return path;
  }
  return path.replace(/\/+$/, "") || "/";
}

function matchesRoute(routePath: string, pathname: string) {
  const routeSegments = normalizePath(routePath).split("/").filter(Boolean);
  const pathSegments = normalizePath(pathname).split("/").filter(Boolean);

  if (routeSegments.length !== pathSegments.length) {
    return false;
  }

  return routeSegments.every((segment, index) => {
    if (segment.startsWith("[") && segment.endsWith("]")) {
      return Boolean(pathSegments[index]);
    }
    return segment === pathSegments[index];
  });
}

function routeSpecificity(route: RouteMeta) {
  return route.path
    .split("/")
    .filter(Boolean)
    .reduce((score, segment) => score + (segment.startsWith("[") ? 1 : 10), 0);
}

export function findRouteMeta(pathname: string) {
  return appRoutes
    .filter((route) => matchesRoute(route.path, pathname))
    .sort((left, right) => routeSpecificity(right) - routeSpecificity(left))[0];
}

export function isRegisteredRoutePath(pathname: string) {
  return Boolean(findRouteMeta(pathname));
}

export function getPortalNavigationRoutes(status: AuthStatus, user: CurrentUser | null) {
  return appRoutes
    .filter((route) => route.navLabel)
    .filter((route) => {
      if (route.public) {
        return true;
      }
      return status === "authenticated" && hasUserPermission(user, route.permission);
    })
    .sort((left, right) => (left.navOrder ?? 0) - (right.navOrder ?? 0));
}

export function resolvePostLoginPath(
  user: CurrentUser,
  requestedPath: string | null | undefined,
  preferredPath?: string,
) {
  const safeRequestedPath = resolveSafeReturnUrl(requestedPath, "");
  if (safeRequestedPath && canAccessPath(user, safeRequestedPath)) {
    return safeRequestedPath;
  }

  if (preferredPath && canAccessPath(user, preferredPath)) {
    return preferredPath;
  }

  if (hasUserPermission(user, "auth:center:view")) {
    return "/dashboard";
  }

  if (hasUserPermission(user, "dashboard:view")) {
    return "/";
  }

  return getPortalNavigationRoutes("authenticated", user)[0]?.path || "/profile";
}

function canAccessPath(user: CurrentUser, path: string) {
  const pathname = new URL(path, "https://bdis.local").pathname;
  const route = findRouteMeta(pathname);
  if (!route || route.path === "/login" || route.path === "/forbidden") {
    return false;
  }
  return Boolean(route.public || hasUserPermission(user, route.permission));
}
