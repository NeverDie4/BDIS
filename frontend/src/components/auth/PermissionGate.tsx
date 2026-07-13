"use client";

import { useAuthStore } from "@/stores/auth-store";

type PermissionGateProps = {
  permission?: string;
  anyOf?: string[];
  allOf?: string[];
  fallback?: React.ReactNode;
  children: React.ReactNode;
};

function includesPermission(userPermissions: string[], permission: string) {
  return userPermissions.includes("*") || userPermissions.includes(permission);
}

export function PermissionGate({
  permission,
  anyOf,
  allOf,
  fallback = null,
  children,
}: PermissionGateProps) {
  const user = useAuthStore((state) => state.user);
  const permissions = user?.permissions ?? [];
  const allowed =
    (!permission || includesPermission(permissions, permission)) &&
    (!anyOf?.length || anyOf.some((item) => includesPermission(permissions, item))) &&
    (!allOf?.length || allOf.every((item) => includesPermission(permissions, item)));

  return allowed ? children : fallback;
}
