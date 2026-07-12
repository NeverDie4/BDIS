"use client";

import { useMemo } from "react";
import { settingTabs } from "@/config/settings/registry";
import { useAuthStore } from "@/stores/auth-store";

export function useVisibleSettingTabs() {
  const user = useAuthStore((state) => state.user);
  return useMemo(() => {
    if (user?.mustChangePassword) {
      return settingTabs.filter((tab) => tab.key === "security");
    }
    return settingTabs
      .filter((tab) => {
        if (!tab.roles?.length && !tab.anyPermissions?.length) return true;
        const roleMatch = tab.roles?.some((role) => user?.roleCodes.includes(role)) ?? false;
        const permissionMatch =
          tab.anyPermissions?.some(
            (permission) =>
              user?.permissions.includes("*") || user?.permissions.includes(permission),
          ) ?? false;
        return roleMatch || permissionMatch;
      })
      .sort((left, right) => left.order - right.order);
  }, [user]);
}
