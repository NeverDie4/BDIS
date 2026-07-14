"use client";

import { useAuthStore } from "@/stores/auth-store";

export function usePermission(permission: string) {
  return useAuthStore((state) => state.hasPermission(permission));
}

export function useAnyPermission(permissions: string[]) {
  return useAuthStore((state) => permissions.some(state.hasPermission));
}

export function useAllPermissions(permissions: string[]) {
  return useAuthStore((state) => permissions.every(state.hasPermission));
}
