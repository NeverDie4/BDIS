"use client";

import { SettingsShell } from "@/components/settings/SettingsShell";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import pageStyles from "./page.module.css";

export default function SettingsPage() {
  return (
    <SiteLayout contentMode="fluid">
      <div className={pageStyles.settingsPage}>
        <ModuleHeroBanner
          description="管理个人资料、账号安全与使用偏好。"
          sealText="PERSONAL SETTINGS"
          title="个人设置"
        />
        <SettingsShell />
      </div>
    </SiteLayout>
  );
}
