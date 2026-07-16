"use client";

import { useEffect } from "react";
import { useSettingNamespace } from "@/hooks/settings/useSettingNamespace";
import { useAuthStore } from "@/stores/auth-store";
import type { AppearanceSettings } from "@/types/settings";

export function AppearancePreferenceBridge() {
  const preferenceEnabled = useAuthStore(
    (state) => state.status === "authenticated" && !state.user?.mustChangePassword,
  );
  const appearance = useSettingNamespace<AppearanceSettings>("appearance", preferenceEnabled);
  const reduceMotion = appearance.data?.values.reduceMotion ?? false;

  useEffect(() => {
    if (reduceMotion) document.documentElement.dataset.reduceMotion = "true";
    else delete document.documentElement.dataset.reduceMotion;
  }, [reduceMotion]);

  return null;
}
