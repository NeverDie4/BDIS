import type { ComponentType } from "react";

export type SettingTabDefinition = {
  key: string;
  label: string;
  order: number;
  icon: ComponentType<{ size?: number }>;
  roles?: string[];
  anyPermissions?: string[];
  component: ComponentType;
};
