import {
  Bell,
  BookOpenCheck,
  GraduationCap,
  Palette,
  ScanSearch,
  ShieldCheck,
  Sprout,
  UserRound,
} from "lucide-react";
import { AppearanceSettings } from "@/components/settings/sections/AppearanceSettings";
import { NotificationSettings } from "@/components/settings/sections/NotificationSettings";
import { ProfileSettings } from "@/components/settings/sections/ProfileSettings";
import {
  CollectionSettings,
  LearningSettings,
  ReviewSettings,
  TeachingSettings,
} from "@/components/settings/sections/RoleExtensionSettings";
import { SecuritySettings } from "@/components/settings/sections/SecuritySettings";
import type { SettingTabDefinition } from "./types";

export const settingTabs: SettingTabDefinition[] = [
  { key: "profile", label: "个人资料", order: 10, icon: UserRound, component: ProfileSettings },
  { key: "security", label: "账号安全", order: 20, icon: ShieldCheck, component: SecuritySettings },
  { key: "appearance", label: "界面偏好", order: 30, icon: Palette, component: AppearanceSettings },
  { key: "notification", label: "通知偏好", order: 40, icon: Bell, component: NotificationSettings },
  { key: "teaching", label: "教学设置", order: 50, icon: GraduationCap, roles: ["TEACHER"], component: TeachingSettings },
  { key: "learning", label: "学习设置", order: 60, icon: BookOpenCheck, roles: ["STUDENT"], component: LearningSettings },
  { key: "collection", label: "采集设置", order: 70, icon: Sprout, roles: ["COLLECTOR"], component: CollectionSettings },
  { key: "review", label: "审核设置", order: 80, icon: ScanSearch, roles: ["REVIEWER"], component: ReviewSettings },
];
