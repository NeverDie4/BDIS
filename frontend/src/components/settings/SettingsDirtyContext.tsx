"use client";

import { createContext, useContext } from "react";

export type SettingsDirtyContextValue = {
  setSectionDirty: (section: string, dirty: boolean) => void;
};

export const SettingsDirtyContext = createContext<SettingsDirtyContextValue | null>(null);

export function useSettingsDirty(section: string) {
  const context = useContext(SettingsDirtyContext);
  return (dirty: boolean) => context?.setSectionDirty(section, dirty);
}
