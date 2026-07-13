"use client";

import { App, Spin } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useVisibleSettingTabs } from "@/hooks/settings/useVisibleSettingTabs";
import { SettingsDirtyContext } from "./SettingsDirtyContext";
import styles from "./settings.module.css";

export function SettingsShell() {
  const { modal } = App.useApp();
  const tabs = useVisibleSettingTabs();
  const [dirtySections, setDirtySections] = useState<Set<string>>(() => new Set());
  const [activeKey, setActiveKey] = useState(() => {
    if (typeof window === "undefined") return "profile";
    return new URLSearchParams(window.location.search).get("tab") || "profile";
  });
  const activeTab = tabs.find((tab) => tab.key === activeKey) ?? tabs[0];
  const hasUnsavedChanges = dirtySections.size > 0;
  const setSectionDirty = useCallback((section: string, dirty: boolean) => {
    setDirtySections((current) => {
      const next = new Set(current);
      if (dirty) next.add(section);
      else next.delete(section);
      return next;
    });
  }, []);
  const dirtyContextValue = useMemo(() => ({ setSectionDirty }), [setSectionDirty]);

  useEffect(() => {
    if (!hasUnsavedChanges) return;
    const beforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    const interceptLink = (event: MouseEvent) => {
      const target = event.target as Element | null;
      const anchor = target?.closest("a[href]") as HTMLAnchorElement | null;
      if (!anchor || anchor.target === "_blank" || anchor.href === window.location.href) return;
      event.preventDefault();
      event.stopPropagation();
      modal.confirm({
        title: "有未保存的修改",
        content: "离开后，本页尚未保存的内容将丢失。",
        okText: "放弃修改并离开",
        cancelText: "继续编辑",
        onOk: () => {
          window.location.href = anchor.href;
        },
      });
    };
    window.addEventListener("beforeunload", beforeUnload);
    document.addEventListener("click", interceptLink, true);
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
      document.removeEventListener("click", interceptLink, true);
    };
  }, [hasUnsavedChanges, modal]);

  function selectTab(nextKey: string) {
    if (nextKey === activeTab?.key) return;
    if (!hasUnsavedChanges) {
      setActiveKey(nextKey);
      replaceTabUrl(nextKey);
      return;
    }
    modal.confirm({
      title: "有未保存的修改",
      content: "切换设置分区后，本页尚未保存的内容将丢失。",
      okText: "放弃修改并切换",
      cancelText: "继续编辑",
      onOk: () => {
        setDirtySections(new Set());
        setActiveKey(nextKey);
        replaceTabUrl(nextKey);
      },
    });
  }

  if (!activeTab) return <Spin />;
  const ActiveComponent = activeTab.component;
  return (
    <SettingsDirtyContext.Provider value={dirtyContextValue}>
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
                  onClick={() => selectTab(tab.key)}
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
    </SettingsDirtyContext.Provider>
  );
}

function replaceTabUrl(tab: string) {
  const url = new URL(window.location.href);
  url.searchParams.set("tab", tab);
  window.history.replaceState(window.history.state, "", `${url.pathname}${url.search}`);
}
