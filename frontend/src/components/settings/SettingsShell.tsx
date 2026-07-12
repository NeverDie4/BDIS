"use client";

import { Spin } from "antd";
import { useState } from "react";
import { useVisibleSettingTabs } from "@/hooks/settings/useVisibleSettingTabs";
import styles from "./settings.module.css";

export function SettingsShell() {
  const tabs = useVisibleSettingTabs();
  const [activeKey, setActiveKey] = useState(() => {
    if (typeof window === "undefined") return "profile";
    return new URLSearchParams(window.location.search).get("tab") || "profile";
  });
  const activeTab = tabs.find((tab) => tab.key === activeKey) ?? tabs[0];

  if (!activeTab) return <Spin />;
  const ActiveComponent = activeTab.component;
  return (
    <div className={styles.shell}>
      <aside className={styles.navigation}>
        <nav className={styles.tabList} aria-label="设置分区">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                className={tab.key === activeTab.key ? styles.tabActive : styles.tab}
                key={tab.key}
                type="button"
                onClick={() => setActiveKey(tab.key)}
              >
                <Icon size={18} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </aside>
      <main className={styles.content} key={activeTab.key}>
        <ActiveComponent />
      </main>
    </div>
  );
}
