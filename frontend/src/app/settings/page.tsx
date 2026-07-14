"use client";

import { SettingsShell } from "@/components/settings/SettingsShell";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import styles from "@/styles/mockPages.module.css";

export default function SettingsPage() {
  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner sealText="PERSONAL SETTINGS" title="个人设置" subtitle="管理个人资料、账号安全与使用偏好。" />
        <SettingsShell />
      </div>
    </SiteLayout>
  );
}
