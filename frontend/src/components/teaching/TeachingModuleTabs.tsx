import { Tabs } from "antd";
import type { TeachingTabKey } from "./types";
import styles from "./teaching.module.css";

type TeachingModuleTabsProps = {
  activeTab: TeachingTabKey;
  onTabChange: (key: TeachingTabKey) => void;
};

export function TeachingModuleTabs({ activeTab, onTabChange }: TeachingModuleTabsProps) {
  return (
    <div className={styles.moduleTabs}>
      <Tabs
        activeKey={activeTab}
        items={[
          { key: "course", label: "实验课程" },
          { key: "research", label: "课题研究" },
          { key: "training", label: "教学培训" },
        ]}
        onChange={(key) => onTabChange(key as TeachingTabKey)}
      />
    </div>
  );
}
